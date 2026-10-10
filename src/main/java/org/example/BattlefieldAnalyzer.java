package org.example;

import org.example.rust.RustBattle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 战场空间（阵型受创）分析。
 *
 * <p>两阶段：先跑「原始阵型 × 测试阵集」的结果并按筛选条件取子集；
 * 再对子集运行 Rust 遥测，把事件归到玩家兵玉/要塞壁的阵型槽位（受创/阵亡），
 * 同时聚合推进曲线（双方 HP/存活/弹幕按时间分箱）与逐局标量（首次接触、核心受击等）。
 *
 * <p>槽位坐标采用阵型工作台标准：兵玉 = 原始码坐标 − 16/−20，核心 = − 52/−58。
 * 需要 Rust 模拟器（遥测无 Java 回退）。
 */
public final class BattlefieldAnalyzer {

    /** 筛选条件。 */
    public enum Filter { ALL, WIN, LOSE, DRAW, TIMEOUT }

    /** 进度回调；phase 0=结果阶段，1=遥测阶段。可能在任意线程触发，GUI 侧需自行切换线程。 */
    public interface ProgressListener {
        void onProgress(int phase, int done, int total);
    }

    /** 槽位元数据；坐标与阵型工作台一致（兵玉 0..348×0..349，核心 0..276）。 */
    public record SlotMeta(int index, int type, int x, int y, int r, boolean core) {}

    // —— 逐局标量下标（与 gkt-jni telemetry::SCALARS 顺序一致）——
    public static final int S_FIRST_COLLISION = 0;
    public static final int S_FIRST_P1_CORE = 1;
    public static final int S_FIRST_P2_CORE = 2;
    public static final int S_COLLISION_COUNT = 3;
    public static final int S_DAMAGE_P1 = 4;
    public static final int S_DAMAGE_P2 = 5;
    public static final int S_CORE_DAMAGE_P1 = 6;
    public static final int S_CORE_DAMAGE_P2 = 7;
    public static final int S_DEATHS_P1 = 8;
    public static final int S_DEATHS_P2 = 9;
    public static final int SCALAR_COUNT = 10;

    /** 时间线每箱列数：sum hp0/hp1/存活0/存活1/弹幕0/弹幕1/样本数。 */
    public static final int TIMELINE_COLS = 7;
    /** 时间线分组数（0=我方胜 / 1=我方负 / 2=其他）。 */
    public static final int GROUPS = 3;

    /** 负载头长度（games/bins/binFrames/slots/SCALARS/GROUPS/TIMELINE_COLS）。 */
    private static final int HEADER = 7;
    /** 遥测分片大小：便于取消与进度刷新。 */
    private static final int CHUNK = 128;

    public record Report(int games, int bins, int binFrames, int slots,
                         double[] scalars, double[] timeline,
                         double[] slotDamage, double[] slotDeaths,
                         List<SlotMeta> metas, List<Result> subsetResults,
                         Filter filter, int evalTotal, long elapsedMs) {

        public double scalar(int game, int index) {
            return scalars[game * SCALAR_COUNT + index];
        }

        public double timelineValue(int group, int bin, int col) {
            return timeline[((group * bins) + bin) * TIMELINE_COLS + col];
        }

        /** 时间线某箱的样本数（有对局推进到该箱的局数）。 */
        public double timelineCount(int group, int bin) {
            return timelineValue(group, bin, TIMELINE_COLS - 1);
        }
    }

    private BattlefieldAnalyzer() {}

    public static Report analyze(String playerText, String evalText, Filter filter, int threads,
                                 int binFrames, ProgressListener listener,
                                 BooleanSupplier cancelled) throws Exception {
        long startNanos = System.nanoTime();
        if (!RustBattle.available()) {
            throw new IllegalStateException(I18n.t("battlefield.err.needRust"));
        }
        int runThreads = Math.max(1, Math.min(256, threads));
        int bin = Math.max(1, binFrames);

        List<Fort> playerRaws = Setting.parseFortsRaw(playerText);
        if (playerRaws.size() != 1) {
            throw new IllegalArgumentException(I18n.t("matchup.err.oneFort"));
        }
        Fort playerRaw = playerRaws.get(0);
        List<Fort> evalRaws = Setting.parseFortsRaw(evalText);
        if (evalRaws.isEmpty()) {
            throw new IllegalArgumentException(I18n.t("matchup.err.emptyEval"));
        }
        int evalTotal = evalRaws.size();

        // —— 第一阶段：结果（用于筛选）——
        String[] allCodes = new String[evalTotal];
        for (int i = 0; i < evalTotal; i++) {
            allCodes[i] = evalRaws.get(i).code();
        }
        List<Result> results = RustBattle.runBatch(new String[]{playerRaw.code()}, allCodes,
                Main.MAX_FRAME_LIMIT, runThreads,
                listener == null ? null : (done, total) -> listener.onProgress(0, done, total));
        if (cancelled != null && cancelled.getAsBoolean()) {
            return null;
        }

        List<String> subCodes = new ArrayList<>();
        List<Result> subResults = new ArrayList<>();
        List<Integer> subGroups = new ArrayList<>();
        for (int i = 0; i < evalTotal; i++) {
            Result r = results.get(i);
            if (!matches(filter, r.status)) {
                continue;
            }
            subCodes.add(allCodes[i]);
            subResults.add(r);
            subGroups.add(r.status == 1 ? 0 : r.status == 2 ? 1 : 2);
        }
        if (subCodes.isEmpty()) {
            throw new IllegalArgumentException(I18n.t("battlefield.err.emptyFilter"));
        }

        // —— 第二阶段：遥测（分片执行）——
        double[] scalars = null;
        double[] timeline = null;
        double[] slotDamage = null;
        double[] slotDeaths = null;
        int bins = 0;
        int slots = 0;
        for (int start = 0; start < subCodes.size(); start += CHUNK) {
            int end = Math.min(subCodes.size(), start + CHUNK);
            String[] codes = subCodes.subList(start, end).toArray(new String[0]);
            int[] groups = new int[end - start];
            for (int i = start; i < end; i++) {
                groups[i - start] = subGroups.get(i);
            }
            final int offset = start;
            double[] payload = RustBattle.runBatchTelemetry(playerRaw.code(), codes, groups,
                    Main.MAX_FRAME_LIMIT, runThreads, bin,
                    listener == null ? null
                            : (done, total) -> listener.onProgress(1, offset + done, subCodes.size()));

            int games = (int) payload[0];
            bins = (int) payload[1];
            slots = (int) payload[3];
            if (scalars == null) {
                scalars = new double[0];
                timeline = new double[GROUPS * bins * TIMELINE_COLS];
                slotDamage = new double[slots];
                slotDeaths = new double[slots];
            }
            int at = HEADER;
            int scalarValues = games * SCALAR_COUNT;
            scalars = append(scalars, payload, at, scalarValues);
            at += scalarValues;
            int timelineValues = GROUPS * bins * TIMELINE_COLS;
            for (int i = 0; i < timelineValues; i++) {
                timeline[i] += payload[at + i];
            }
            at += timelineValues;
            for (int i = 0; i < slots; i++) {
                slotDamage[i] += payload[at + i];
                slotDeaths[i] += payload[at + slots + i];
            }
            if (cancelled != null && cancelled.getAsBoolean()) {
                return null;
            }
        }

        // —— 槽位元数据（玩家阵型，工作台坐标）——
        CompiledFort cf = Main.compileFort(playerRaw);
        List<SlotMeta> metas = new ArrayList<>(cf.unitCount + 1);
        for (int k = 0; k < cf.unitCount; k++) {
            metas.add(new SlotMeta(k, cf.type[k], cf.x[k] - 16, cf.y[k] - 20, cf.r[k], false));
        }
        // 核心贴图 id 与 Unit.decodeAsCore 一致：核心类型 0/1/2 → 0/61/62
        int coreSprite = cf.coreType == 1 ? 61 : (cf.coreType == 2 ? 62 : 0);
        metas.add(new SlotMeta(cf.unitCount, coreSprite,
                cf.coreX + 190 - 52, cf.coreY + 400 - 58, 0, true));

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        return new Report(scalars.length / SCALAR_COUNT, bins, bin, slots,
                scalars, timeline, slotDamage, slotDeaths,
                metas, subResults, filter, evalTotal, elapsedMs);
    }

    private static boolean matches(Filter filter, int status) {
        return switch (filter) {
            case ALL -> true;
            case WIN -> status == 1;
            case LOSE -> status == 2;
            case DRAW -> status == 0;
            case TIMEOUT -> status == -1;
        };
    }

    private static double[] append(double[] target, double[] source, int offset, int length) {
        double[] out = new double[target.length + length];
        System.arraycopy(target, 0, out, 0, target.length);
        System.arraycopy(source, offset, out, target.length, length);
        return out;
    }
}
