package org.example.rust;

import org.example.BattleCache;
import org.example.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rust 对战模拟器（gkt-core JNI 桥）的 Java 入口。
 *
 * <p>原生库不可用时 {@link #available()} 返回 false，调用方应回退 Java 引擎；
 * 原生库与 gkt-core 一致，采用修正版 oracle 语义（默认种子路径，由双方核心位置决定）。
 */
public final class RustBattle {

    /** 批量进度回调；由 Rust 工作线程调用，实现方需自行切换线程（如 SwingUtilities）。 */
    public interface BatchCallback {
        void onProgress(int done, int total);
    }

    private static Boolean available;
    private static String unavailableReason = "";
    private static String engineVersion = "";

    private RustBattle() {
    }

    /** 原生库是否可用；首次调用触发加载。 */
    public static synchronized boolean available() {
        if (available == null) {
            String error = NativeLoader.load();
            if (error == null) {
                try {
                    engineVersion = engineVersionNative();
                    available = Boolean.TRUE;
                } catch (Throwable t) {
                    available = Boolean.FALSE;
                    unavailableReason = "原生库已加载但初始化失败：" + t;
                }
            } else {
                available = Boolean.FALSE;
                unavailableReason = error;
            }
        }
        return available;
    }

    /** 不可用原因（可用时为空串）。 */
    public static synchronized String unavailableReason() {
        available();
        return unavailableReason;
    }

    /** 引擎标识（如 "gkt-core (bit-exact rust)"）。 */
    public static synchronized String engineVersion() {
        available();
        return engineVersion;
    }

    /** 单局对战（带对局级持久缓存）；阵型代码须为已清洗的 61 进制串（长度 6 的倍数、首字符 0..2）。 */
    public static Result runSingle(String code1, String code2, int maxFrames) {
        ensureAvailable();
        String engine = engineKey();
        Result cached = BattleCache.get(engine, code1, code2, maxFrames);
        if (cached != null) {
            return cached;
        }
        double[] values = runSingleNative(code1, code2, maxFrames);
        Result result = toResult(values, 0);
        BattleCache.put(engine, code1, code2, maxFrames, result);
        return result;
    }

    /**
     * 批量对战：1P 外层 × 2P 内层（与 Java 引擎的 futures 顺序一致），返回顺序同 {@code codes1 × codes2}。
     *
     * <p>带对局级持久缓存：全部命中时直接返回；全部未命中时走原生批量（最快）并写缓存；
     * 部分命中时只补跑缺失对局（线程池并行，逐局走 {@link #runSingle}）。
     *
     * @param callback 进度回调，可空
     */
    public static List<Result> runBatch(String[] codes1, String[] codes2, int maxFrames,
                                        int threads, BatchCallback callback) {
        ensureAvailable();
        String engine = engineKey();
        int n1 = codes1.length;
        int n2 = codes2.length;
        int total = n1 * n2;
        Result[] out = new Result[total];
        List<Integer> misses = new ArrayList<>();
        for (int i = 0; i < n1; i++) {
            for (int j = 0; j < n2; j++) {
                int idx = i * n2 + j;
                Result cached = BattleCache.get(engine, codes1[i], codes2[j], maxFrames);
                if (cached != null) {
                    out[idx] = cached;
                } else {
                    misses.add(idx);
                }
            }
        }
        if (misses.isEmpty()) {
            if (callback != null) {
                callback.onProgress(total, total);
            }
            return toList(out);
        }
        if (misses.size() == total) {
            double[] flat = runBatchNative(codes1, codes2, maxFrames, threads, callback);
            for (int k = 0; k < total; k++) {
                Result result = toResult(flat, k * 4);
                out[k] = result;
                BattleCache.put(engine, codes1[k / n2], codes2[k % n2], maxFrames, result);
            }
            return toList(out);
        }
        int hits = total - misses.size();
        AtomicInteger done = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(
                Math.max(1, Math.min(threads, misses.size())));
        try {
            List<Future<?>> futures = new ArrayList<>(misses.size());
            for (int idx : misses) {
                final int i = idx / n2;
                final int j = idx % n2;
                futures.add(pool.submit(() -> {
                    try {
                        out[idx] = runSingle(codes1[i], codes2[j], maxFrames);
                    } catch (Throwable t) {
                        out[idx] = new Result(-2, 0, 0, 0f);
                    }
                    if (callback != null) {
                        callback.onProgress(hits + done.incrementAndGet(), total);
                    }
                }));
            }
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception ignored) {
                    // 单局失败已在任务内兜底
                }
            }
        } finally {
            pool.shutdownNow();
        }
        return toList(out);
    }

    private static List<Result> toList(Result[] array) {
        List<Result> list = new ArrayList<>(array.length);
        for (Result r : array) {
            list.add(r);
        }
        return list;
    }

    /** 对局缓存键的引擎标识（含引擎版本，DLL 更新自动失效）。 */
    private static String engineKey() {
        return "rust|" + engineVersion();
    }

    /**
     * 战场空间遥测批量：对玩家阵型（1P）×测试阵集子集跑带遥测的对局，
     * 返回紧凑聚合负载（结构见 {@link org.example.BattlefieldAnalyzer}）。
     *
     * @param groups 每局分组（0=我方胜 / 1=我方负 / 2=其他），用于推进曲线分组平均
     */
    public static double[] runBatchTelemetry(String playerCode, String[] evalCodes, int[] groups,
                                             int maxFrames, int threads, int binFrames,
                                             BatchCallback callback) {
        ensureAvailable();
        return runBatchTelemetryNative(playerCode, evalCodes, groups, maxFrames, threads,
                binFrames, callback);
    }

    private static Result toResult(double[] values, int offset) {
        return new Result(
                (int) values[offset],
                (int) values[offset + 1],
                (int) values[offset + 2],
                (float) values[offset + 3]);
    }

    private static void ensureAvailable() {
        if (!available()) {
            throw new IllegalStateException("Rust 模拟器不可用：" + unavailableReason);
        }
    }

    private static native double[] runSingleNative(String code1, String code2, int maxFrames);

    private static native double[] runBatchNative(String[] codes1, String[] codes2,
                                                  int maxFrames, int threads, BatchCallback callback);

    private static native double[] runBatchTelemetryNative(String playerCode, String[] evalCodes,
                                                           int[] groups, int maxFrames, int threads,
                                                           int binFrames, BatchCallback callback);

    private static native String engineVersionNative();
}
