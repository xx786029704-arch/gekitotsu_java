package org.example;

import org.example.GUI.Formation;
import org.example.GUI.Unit;
import org.example.rust.RustBattle;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;

/**
 * 对阵画像：从结果级数据统计原始阵型面对测试阵集时的特征。
 *
 * <p>只跑「原始阵型 × 测试阵集」的对战并聚合，不修改模拟引擎、不采集遥测。
 * 统计口径与贡献分析一致：胜率 = (2×胜 + 平) × 50% /（胜+负+平），超时与异常不计入。
 *
 * <p>Keener 修正（可选）：对测试阵集内部跑循环赛（每对双方向各一局），Keener 矩阵取每对双局
 * 聚合得分 {@code sIJ + 1 − sJI} 再加 1 基线（保证矩阵严格正、主特征向量唯一），用幂迭代
 * 求 Perron–Frobenius 主特征向量作为对手强度权重；阵型/我方的修正胜率为按对手权重加权平均
 * 的每局得分率 {@code ρ = Σ w·得分 / Σ w}（0.5 = 阵集平均）。我方复用阶段一全部对局，
 * 不新增对战、也不作为矩阵节点（采样路径下阵集节点平均只打 d 局而我方打满 m 局，
 * 基线会随对局数系统性抬高我方权重）。
 */
public final class MatchupAnalyzer {

    /** 进度回调；可能在分析线程或 Rust 工作线程触发，GUI 侧需自行切换线程。 */
    public interface ProgressListener {
        void onProgress(int done, int total);

        /** 阶段切换提示（参数为 lang.json 词条键）；默认忽略。 */
        default void onStage(String labelKey) {}
    }

    /** 单个测试阵型的元数据；坐标采用阵型工作台标准（核心 = 原始码坐标 − 52/−58，范围 0..276）。 */
    public record FortMeta(String name, int coreType, int coreX, int coreY, int[] unitTypes) {}

    /**
     * 单位敏感度：该兵玉在测试阵集中出现 / 未出现时的我方胜率与差值（百分点）。
     *
     * <p>出现次数少的单位差值不可靠，因此额外给出 {@code adjustedDelta}：两组的胜率先各自
     * 向 50% 收缩（伪计数 {@value #SHRINK_PRIOR} 局），再相减；样本越少收缩越强，排名应以此为准。
     */
    public record SensitivityRow(int type, int samples,
                                 int withWins, int withDecisive, double withRate,
                                 int withoutWins, int withoutDecisive, double withoutRate,
                                 double delta, double adjustedDelta) {}

    /** 低样本差值收缩的伪计数局数。 */
    private static final int SHRINK_PRIOR = 4;

    /** 加速度相性：对手阵型按加速度等级（红加速器 +1、蓝加速器 +2）聚合的胜负。 */
    public record AccelRow(int level, int samples, int wins, int loses, int draws, double winRate) {}

    /**
     * Keener 修正：单个对手的条目。rate 为该对手的修正胜率（按 Keener 权重加权平均的每局得分率，
     * 0.5 = 阵集平均，百分数）；rank 为含我方在内的名次（1 起）。
     */
    public record StrengthEntry(int index, String name, double rate, int rank, int status) {}

    /**
     * Keener 修正结果：阵集用循环赛 Keener 权重 w（对手越强权重越高），阵型/我方的修正胜率
     * 为按对手权重加权平均的每局得分率：{@code ρ = Σ w·得分 / Σ w}（0.5 = 阵集平均）。
     * 我方复用阶段一全部对局，不新增对战；对外只暴露胜率口径。
     *
     * <p>poolSize 为测试阵集数量；games 为计入的对局数；targetRank 为含我方在内的名次；
     * actualRate 为对阵集实际胜率；correctedRate 为加权修正胜率。均为百分数。
     */
    public record StrengthReport(List<StrengthEntry> entries, int poolSize, int games,
                                 int targetRank, double actualRate, double correctedRate) {
        /** 变化量 = 修正胜率 − 实际胜率（无有效数据时为 NaN）。 */
        public double delta() {
            return Double.isNaN(actualRate) || Double.isNaN(correctedRate)
                    ? Double.NaN : correctedRate - actualRate;
        }
    }

    /** 分析结果：逐局原始数据 + 阵集元数据 + 敏感度排行 + 加速度相性 + 强度相性（可空）。 */
    public record Report(int total, int win, int lose, int draw, int timeout, int battleErrors,
                         double winRate,
                         int[] statuses, int[] frames, int[] winnerHp,
                         List<FortMeta> evalForts, List<SensitivityRow> sensitivity,
                         List<AccelRow> acceleration, StrengthReport strength,
                         long elapsedMs) {}

    /** Rust 批量分片大小：便于在分片之间响应取消与刷新进度。 */
    private static final int RUST_CHUNK = 256;

    /** Keener 修正的最小阵集规模；小于该值样本不足。 */
    public static final int STRENGTH_MIN_POOL = 6;

    /** 阵集循环赛对战预算（每对双方向两局）；超出时按规则圆环图确定性采样。 */
    private static final int STRENGTH_MAX_BATTLES = 160000;

    /** Keener 矩阵中未对局配对的伪权重：保证矩阵严格正，Perron–Frobenius 主特征向量唯一。 */
    private static final double KEENER_EPS = 1e-6;

    /** Keener 幂迭代的最大次数与相邻迭代向量的相对收敛阈值。 */
    private static final int KEENER_MAX_ITER = 2000;
    private static final double KEENER_TOL = 1e-13;

    /** 阵集 Keener 权重 + 修正胜率的持久缓存（仅由循环赛决定，与原始阵型无关，可跨进程复用）。 */
    private static final StrengthCache STRENGTH_CACHE = new StrengthCache();

    /**
     * 阵集循环赛结果：pi/pj 为阵集内索引；sIJ 为 pi 作为 1P 对 pj 的得分、
     * sJI 为 pj 作为 1P 对 pi 的得分（1=胜 / 0.5=平 / 0=负，超时与异常按平局计）。
     */
    private record PoolResults(int[] pi, int[] pj, double[] sIJ, double[] sJI) {}

    /**
     * 阵集评分：weights 为 Keener Perron 权重（Σ=1）、rates 为按权重加权平均的修正胜率（0..1），
     * 均按阵集列表顺序、与原始阵型无关。
     */
    private record PoolRating(double[] weights, double[] rates) {}

    private MatchupAnalyzer() {}

    public static Report analyze(String playerText, String evalText, int threads, boolean estimateStrength,
                                 ProgressListener listener, BooleanSupplier cancelled) throws Exception {
        long startNanos = System.nanoTime();

        List<Fort> playerRaws = Setting.parseFortsRaw(playerText);
        if (playerRaws.size() != 1) {
            throw new IllegalArgumentException(I18n.t("matchup.err.oneFort"));
        }
        Fort playerRaw = playerRaws.get(0);
        List<Fort> evalRaws = Setting.parseFortsRaw(evalText);
        if (evalRaws.isEmpty()) {
            throw new IllegalArgumentException(I18n.t("matchup.err.emptyEval"));
        }
        int m = evalRaws.size();
        int runThreads = Math.max(1, Math.min(256, threads));
        long[] rrPairs = estimateStrength && m >= STRENGTH_MIN_POOL ? samplePairs(m) : null;
        // 命中循环赛缓存时不再对战：进度总量只算阶段一，且不显示「Keener 修正计算中」
        PoolRating cachedRating = rrPairs == null ? null : strengthCacheGet(evalRaws);
        int grandTotal = m + (rrPairs == null || cachedRating != null ? 0 : 2 * rrPairs.length);

        List<FortMeta> metas = new ArrayList<>(m);
        for (Fort fort : evalRaws) {
            CompiledFort cf = Main.compileFort(fort);
            // 工作台标准：核心坐标 = 原始码坐标 − 52/−58（与 Unit.decodeAsCore 一致）
            metas.add(new FortMeta(fort.name(), cf.coreType,
                    cf.coreX + 190 - 52, cf.coreY + 400 - 58, cf.type.clone()));
        }

        List<Result> results = runBattles(playerRaw, evalRaws, runThreads, grandTotal, listener, cancelled);
        if (results == null) {
            return null; // 已取消
        }

        int[] statuses = new int[m];
        int[] frames = new int[m];
        int[] winnerHp = new int[m];
        int win = 0, lose = 0, draw = 0, timeout = 0, battleErrors = 0;

        int typeCount = Unit.infos.length;
        int[] withWins = new int[typeCount];
        int[] withLoses = new int[typeCount];
        int[] withDraws = new int[typeCount];
        int[] withSamples = new int[typeCount];
        int globalWins = 0, globalLoses = 0, globalDraws = 0;

        for (int i = 0; i < m; i++) {
            Result r = results.get(i);
            statuses[i] = r.status;
            frames[i] = r.framePassed;
            winnerHp[i] = r.winnerHp;
            switch (r.status) {
                case 1 -> win++;
                case 2 -> lose++;
                case 0 -> draw++;
                case -1 -> timeout++;
                default -> battleErrors++;
            }
            boolean countable = r.status == 0 || r.status == 1 || r.status == 2;
            if (countable) {
                if (r.status == 1) {
                    globalWins++;
                } else if (r.status == 2) {
                    globalLoses++;
                } else {
                    globalDraws++;
                }
            }
            boolean[] seen = new boolean[typeCount];
            for (int type : metas.get(i).unitTypes()) {
                if (type < 0 || type >= typeCount || seen[type]) {
                    continue;
                }
                seen[type] = true;
                withSamples[type]++;
                if (countable) {
                    if (r.status == 1) {
                        withWins[type]++;
                    } else if (r.status == 2) {
                        withLoses[type]++;
                    } else {
                        withDraws[type]++;
                    }
                }
            }
        }

        List<SensitivityRow> sensitivity = new ArrayList<>();
        for (int type = 0; type < typeCount; type++) {
            if (withSamples[type] == 0) {
                continue;
            }
            double withRate = winRate(withWins[type], withLoses[type], withDraws[type]);
            int withDecisive = withWins[type] + withLoses[type] + withDraws[type];
            int withoutWins = globalWins - withWins[type];
            int withoutLoses = globalLoses - withLoses[type];
            int withoutDraws = globalDraws - withDraws[type];
            int withoutDecisive = withoutWins + withoutLoses + withoutDraws;
            double withoutRate = winRate(withoutWins, withoutLoses, withoutDraws);
            double delta = (Double.isNaN(withRate) || Double.isNaN(withoutRate))
                    ? Double.NaN : withRate - withoutRate;
            double adjustedDelta = shrink(withRate, withDecisive) - shrink(withoutRate, withoutDecisive);
            sensitivity.add(new SensitivityRow(type, withSamples[type],
                    withWins[type], withDecisive, withRate,
                    withoutWins, withoutDecisive, withoutRate,
                    delta, adjustedDelta));
        }

        // —— 加速度相性：对手阵型按加速度等级聚合（口径同 Formation.getAccelLevel）——
        int[] levels = new int[m];
        int maxLevel = 0;
        for (int i = 0; i < m; i++) {
            levels[i] = Formation.accelLevelOf(metas.get(i).unitTypes());
            maxLevel = Math.max(maxLevel, levels[i]);
        }
        int[] accelSamples = new int[maxLevel + 1];
        int[] accelWins = new int[maxLevel + 1];
        int[] accelLoses = new int[maxLevel + 1];
        int[] accelDraws = new int[maxLevel + 1];
        for (int i = 0; i < m; i++) {
            int lv = levels[i];
            accelSamples[lv]++;
            switch (statuses[i]) {
                case 1 -> accelWins[lv]++;
                case 2 -> accelLoses[lv]++;
                case 0 -> accelDraws[lv]++;
                default -> { }
            }
        }
        List<AccelRow> acceleration = new ArrayList<>();
        for (int lv = 0; lv <= maxLevel; lv++) {
            if (accelSamples[lv] == 0) {
                continue;
            }
            acceleration.add(new AccelRow(lv, accelSamples[lv],
                    accelWins[lv], accelLoses[lv], accelDraws[lv],
                    winRate(accelWins[lv], accelLoses[lv], accelDraws[lv])));
        }

        // —— Keener 修正：阵集循环赛权重 → 加权平均得分率 → 修正胜率 ——
        StrengthReport strength = null;
        if (rrPairs != null) {
            if (listener != null && cachedRating == null) {
                listener.onStage("matchup.strength.stage");
            }
            strength = computeStrength(evalRaws, statuses, rrPairs, cachedRating,
                    runThreads, listener, cancelled);
            if (strength == null) {
                return null; // 已取消
            }
        }

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        return new Report(m, win, lose, draw, timeout, battleErrors,
                winRate(win, lose, draw), statuses, frames, winnerHp,
                metas, sensitivity, acceleration, strength, elapsedMs);
    }

    /** 运行「原始阵型 × 测试阵集」；取消时返回 null。优先 Rust 批量，不可用时回退 Java 引擎。 */
    private static List<Result> runBattles(Fort playerRaw, List<Fort> evalRaws, int threads, int grandTotal,
                                           ProgressListener listener, BooleanSupplier cancelled) {
        int m = evalRaws.size();
        List<Result> results = new ArrayList<>(m);
        if (RustBattle.available()) {
            for (int start = 0; start < m; start += RUST_CHUNK) {
                int end = Math.min(m, start + RUST_CHUNK);
                final int offset = start;
                String[] codes = new String[end - start];
                for (int i = start; i < end; i++) {
                    codes[i - start] = evalRaws.get(i).code();
                }
                results.addAll(RustBattle.runBatch(new String[]{playerRaw.code()}, codes,
                        Main.MAX_FRAME_LIMIT, threads,
                        listener == null ? null : (done, total) -> listener.onProgress(offset + done, grandTotal)));
                if (cancelled != null && cancelled.getAsBoolean()) {
                    return null;
                }
            }
            return results;
        }

        CompiledFort player = Main.compileFort(playerRaw);
        List<CompiledFort> compiledEval = Setting.compileAll(evalRaws);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Result>> futures = new ArrayList<>(m);
            for (int i = 0; i < m; i++) {
                final int idx = i;
                futures.add(pool.submit(() -> runJavaCached(player, playerRaw.code(),
                        compiledEval.get(idx), evalRaws.get(idx).code())));
            }
            for (int i = 0; i < m; i++) {
                try {
                    results.add(futures.get(i).get());
                } catch (Exception e) {
                    results.add(new Result(-2, -1, 0, 0));
                }
                if (listener != null) {
                    listener.onProgress(i + 1, grandTotal);
                }
                if (cancelled != null && cancelled.getAsBoolean()) {
                    return null;
                }
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    // —— 强度相性实现 ——

    /**
     * 生成循环赛对局（编码为 {@code i * m + j}，i&lt;j）：规模不大时全量，否则按规则圆环图确定性采样。
     *
     * <p>采样取 K 个随机偏移（固定包含偏移 1 保证图连通），每对 (i, (i+偏移) mod m) 都进入赛程——
     * 每个节点的度数恰为 2K，避免"对局数不同导致 Keener 评分被 +1 基线系统抬高"（旧贪心采样
     * 的度分布在 7..49 之间，评分与度数的相关系数高达 0.94）。
     */
    private static long[] samplePairs(int m) {
        long maxPairs = STRENGTH_MAX_BATTLES / 2L;
        long all = (long) m * (m - 1) / 2;
        if (all <= maxPairs) {
            long[] pairs = new long[(int) all];
            int k = 0;
            for (int i = 0; i < m; i++) {
                for (int j = i + 1; j < m; j++) {
                    pairs[k++] = (long) i * m + j;
                }
            }
            return pairs;
        }
        int maxOffset = (m - 1) / 2;
        int count = Math.min(Math.max(1, (int) (maxPairs / m)), maxOffset);
        int extra = count - 1;
        int[] pool = new int[Math.max(0, maxOffset - 1)];
        for (int i = 0; i < pool.length; i++) {
            pool[i] = i + 2;
        }
        Random rnd = new Random(1L);
        for (int i = 0; i < extra; i++) {
            int r = i + rnd.nextInt(pool.length - i);
            int tmp = pool[i];
            pool[i] = pool[r];
            pool[r] = tmp;
        }
        HashSet<Long> set = new HashSet<>();
        for (int i = -1; i < extra; i++) {
            int offset = i < 0 ? 1 : pool[i];
            for (int a = 0; a < m; a++) {
                int b = (a + offset) % m;
                set.add((long) Math.min(a, b) * m + Math.max(a, b));
            }
        }
        long[] pairs = new long[set.size()];
        int k = 0;
        for (long pair : set) {
            pairs[k++] = pair;
        }
        Arrays.sort(pairs);
        return pairs;
    }

    /** 强度标定所需的对战数（双方向，即 2×对数）；阵集过小时返回 0。 */
    public static int strengthBattleEstimate(int count) {
        if (count < STRENGTH_MIN_POOL) {
            return 0;
        }
        long maxPairs = STRENGTH_MAX_BATTLES / 2L;
        long all = (long) count * (count - 1) / 2;
        long pairs;
        if (all <= maxPairs) {
            pairs = all;
        } else {
            int maxOffset = (count - 1) / 2;
            pairs = (long) count * Math.min(Math.max(1, (int) (maxPairs / count)), maxOffset);
        }
        return (int) Math.min(Integer.MAX_VALUE, 2 * pairs);
    }

    /** 由循环赛权重 + 我方对局计算 Keener 修正（含缓存命中路径）；取消时返回 null。 */
    private static StrengthReport computeStrength(List<Fort> evalRaws, int[] targetStatuses,
                                                  long[] pairs, PoolRating cached, int threads,
                                                  ProgressListener listener, BooleanSupplier cancelled) {
        int m = evalRaws.size();
        PoolRating pool;
        if (cached != null) {
            pool = cached;
        } else {
            PoolResults raw = runStrengthBattles(evalRaws, pairs, threads, listener, cancelled);
            if (raw == null) {
                return null;
            }
            pool = fitPoolRating(m, raw);
            STRENGTH_CACHE.put(strengthCacheKey(evalRaws), pool);
        }
        double[] weights = pool.weights();
        double[] rates = pool.rates();

        // 我方修正胜率：按对手 Keener 权重加权平均阶段一每局得分（胜 1 / 平 0.5 / 负 0）。
        // 对手越强权重越高，因此"修正"会相对实际胜率上调或下调——这正是强度修正的含义。
        int win = 0, lose = 0, draw = 0;
        double scoreSum = 0;
        double weightSum = 0;
        for (int i = 0; i < m; i++) {
            int st = targetStatuses[i];
            if (st != 0 && st != 1 && st != 2) {
                continue;
            }
            double score = st == 1 ? 1.0 : (st == 2 ? 0.0 : 0.5);
            scoreSum += weights[i] * score;
            weightSum += weights[i];
            if (st == 1) {
                win++;
            } else if (st == 2) {
                lose++;
            } else {
                draw++;
            }
        }
        double correctedRate = weightSum > 0 ? 100.0 * scoreSum / weightSum : Double.NaN;

        // 名次与逐对手条目：按修正胜率比较（0.5 = 阵集平均）。
        // 对手名次 = 1 + 比它高的对手数 +（我方比它高 ? 1 : 0）；并列不增加。
        int targetRank = weightSum > 0 ? 1 : m + 1;
        List<StrengthEntry> entries = new ArrayList<>(m);
        double targetRate = correctedRate / 100.0;
        for (int i = 0; i < m; i++) {
            int rank = 1;
            for (int j = 0; j < m; j++) {
                if (j != i && !Double.isNaN(rates[j])
                        && (Double.isNaN(rates[i]) || rates[j] > rates[i])) {
                    rank++;
                }
            }
            if (weightSum > 0) {
                if (Double.isNaN(rates[i]) || rates[i] < targetRate) {
                    rank++; // 我方排在对手之前（无有效评分的对手排最后）
                } else if (rates[i] > targetRate) {
                    targetRank++; // 对手排在我方之前
                }
            }
            Fort fort = evalRaws.get(i);
            entries.add(new StrengthEntry(i, fort.name().isEmpty() ? fort.code() : fort.name(),
                    100.0 * rates[i], rank, targetStatuses[i]));
        }
        return new StrengthReport(entries, m, win + lose + draw, targetRank,
                winRate(win, lose, draw), correctedRate);
    }

    /** 仅用阵集循环赛求 Keener 权重，并加权平均双局聚合得分得到阵集修正胜率（目标无关，可缓存）。 */
    private static PoolRating fitPoolRating(int m, PoolResults pool) {
        int nc = pool.pi().length;
        int[] ei = new int[2 * nc];
        int[] ej = new int[2 * nc];
        double[] ew = new double[2 * nc];
        int e = 0;
        for (int k = 0; k < nc; k++) {
            int a = pool.pi()[k];
            int b = pool.pj()[k];
            // 双局聚合得分 = sIJ + 1 − sJI（胜两局 2 / 各胜一局 1 / 全负 0），再 +1 作为
            // Keener 基线使矩阵严格正：否则全量对局的"胜者矩阵"可约（小阵集实测出现零分量）
            double mIJ = pool.sIJ()[k] + 1.0 - pool.sJI()[k];
            ei[e] = a; ej[e] = b; ew[e] = mIJ + 1.0; e++;
            ei[e] = b; ej[e] = a; ew[e] = 2.0 - mIJ + 1.0; e++;
        }
        double[] weights = perronVector(m, ei, ej, ew, e);
        // 修正胜率 ρ_i = Σ_{k≠i} w_k·M_ik / (2·Σ_{k≠i} w_k)，0.5 = 阵集平均
        double[] num = new double[m];
        double[] den = new double[m];
        for (int k = 0; k < nc; k++) {
            int a = pool.pi()[k];
            int b = pool.pj()[k];
            double mIJ = pool.sIJ()[k] + 1.0 - pool.sJI()[k];
            num[a] += mIJ * weights[b];
            den[a] += weights[b];
            num[b] += (2.0 - mIJ) * weights[a];
            den[b] += weights[a];
        }
        double[] rates = new double[m];
        for (int i = 0; i < m; i++) {
            rates[i] = den[i] > 0 ? num[i] / (2.0 * den[i]) : Double.NaN;
        }
        return new PoolRating(weights, rates);
    }

    /**
     * Keener 幂迭代：求严格正矩阵的主特征向量（按分量和归一）。
     *
     * <p>矩阵以稀疏边表给出（已对局配对取双局聚合权重，允许为 0），未对局配对取 {@link #KEENER_EPS}；
     * 迭代等价于 {@code next_i = EPS·Σr + Σ_{j∈E(i)}(w_ij−EPS)·r_j}。
     */
    private static double[] perronVector(int n, int[] ei, int[] ej, double[] ew, int edgeCount) {
        double[] r = new double[n];
        Arrays.fill(r, 1.0 / n);
        double[] next = new double[n];
        for (int iter = 0; iter < KEENER_MAX_ITER; iter++) {
            double sum = 0;
            for (double v : r) {
                sum += v;
            }
            Arrays.fill(next, KEENER_EPS * sum);
            for (int k = 0; k < edgeCount; k++) {
                next[ei[k]] += (ew[k] - KEENER_EPS) * r[ej[k]];
            }
            double total = 0;
            for (double v : next) {
                total += v;
            }
            double maxRel = 0;
            for (int i = 0; i < n; i++) {
                next[i] /= total;
                maxRel = Math.max(maxRel, Math.abs(next[i] - r[i]) / next[i]);
            }
            double[] tmp = r;
            r = next;
            next = tmp;
            if (maxRel < KEENER_TOL) {
                break;
            }
        }
        return r;
    }

    /**
     * 跑阵集循环赛并返回每一对的双方向结果；取消时返回 null。
     * 每对打两局（各自作为 1P 一次），使边权为两局聚合得分、两侧场数完全均衡；
     * 对局在 Java 线程池上并行（Rust 引擎用 runSingle，避免逐行批量的长尾串行）。
     */
    private static PoolResults runStrengthBattles(List<Fort> evalRaws, long[] pairs, int threads,
                                                  ProgressListener listener, BooleanSupplier cancelled) {
        int m = evalRaws.size();
        int n = pairs.length;
        int total = m + 2 * n;
        int[] pi = new int[n];
        int[] pj = new int[n];
        for (int k = 0; k < n; k++) {
            pi[k] = (int) (pairs[k] / m);
            pj[k] = (int) (pairs[k] % m);
        }
        double[] sIJ = new double[n];
        double[] sJI = new double[n];

        boolean rust = RustBattle.available();
        String[] poolCodes = new String[m];
        for (int i = 0; i < m; i++) {
            poolCodes[i] = evalRaws.get(i).code();
        }
        List<CompiledFort> forts = rust ? null : Setting.compileAll(evalRaws);

        int chunkPairs = Math.max(16, threads * 2);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Result>> futures = new ArrayList<>();
            int chunkStart = 0;
            for (int k = 0; k < n; k++) {
                final int a = pi[k];
                final int b = pj[k];
                futures.add(pool.submit(() -> rust
                        ? RustBattle.runSingle(poolCodes[a], poolCodes[b], Main.MAX_FRAME_LIMIT)
                        : runJavaCached(forts.get(a), poolCodes[a], forts.get(b), poolCodes[b])));
                futures.add(pool.submit(() -> rust
                        ? RustBattle.runSingle(poolCodes[b], poolCodes[a], Main.MAX_FRAME_LIMIT)
                        : runJavaCached(forts.get(b), poolCodes[b], forts.get(a), poolCodes[a])));
                if (futures.size() >= 2 * chunkPairs || k == n - 1) {
                    for (int t = 0; t < futures.size(); t++) {
                        int idx = chunkStart + t / 2;
                        double score;
                        try {
                            score = scoreOf(futures.get(t).get(), true);
                        } catch (Exception ignored) {
                            score = Double.NaN;
                        }
                        if (Double.isNaN(score)) {
                            score = 0.5; // 超时/异常按平局计，避免整对缺失
                        }
                        if ((t & 1) == 0) {
                            sIJ[idx] = score;
                        } else {
                            sJI[idx] = score;
                        }
                    }
                    futures.clear();
                    chunkStart = k + 1;
                    if (listener != null) {
                        listener.onProgress(m + 2 * (k + 1), total);
                    }
                    if (cancelled != null && cancelled.getAsBoolean()) {
                        return null;
                    }
                }
            }
        } finally {
            pool.shutdownNow();
        }
        return new PoolResults(pi, pj, sIJ, sJI);
    }

    /** Java 引擎单局（带对局级缓存）：缓存键以 "java" 区分引擎，避免与 Rust 结果混用。 */
    private static Result runJavaCached(CompiledFort a, String codeA, CompiledFort b, String codeB) {
        Result cached = BattleCache.get("java", codeA, codeB, Main.MAX_FRAME_LIMIT);
        if (cached != null) {
            return cached;
        }
        Result result = new GameTask().run_single(a, b);
        if (result != null && result.status != -2) {
            BattleCache.put("java", codeA, codeB, Main.MAX_FRAME_LIMIT, result);
        }
        return result;
    }

    /** 将一局结果换算为节点 i 的得分（1=胜 / 0.5=平 / 0=负）；超时与异常返回 NaN。 */
    private static double scoreOf(Result result, boolean iIsFirst) {
        if (result.status == 0) {
            return 0.5;
        }
        if (result.status == 1) {
            return iIsFirst ? 1.0 : 0.0;
        }
        if (result.status == 2) {
            return iIsFirst ? 0.0 : 1.0;
        }
        return Double.NaN;
    }

    /** 读取阵集评分缓存（返回副本）；未命中返回 null。 */
    private static PoolRating strengthCacheGet(List<Fort> raws) {
        return STRENGTH_CACHE.get(strengthCacheKey(raws));
    }

    /** 缓存键：格式版本 + 引擎 + 帧上限 + 阵集代码，取 SHA-256 十六进制（避免键字符串过长）。 */
    private static String strengthCacheKey(List<Fort> raws) {
        StringBuilder sb = new StringBuilder();
        sb.append("v3|");
        sb.append(RustBattle.available() ? "rust|" + RustBattle.engineVersion() : "java");
        sb.append('|').append(Main.MAX_FRAME_LIMIT);
        for (Fort f : raws) {
            sb.append('|').append(f.code());
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            return "h" + Integer.toHexString(sb.toString().hashCode());
        }
    }

    /**
     * 阵集 Keener 权重与修正胜率的持久缓存：工作目录 {@code strength_cache.bin} 读写，进程重启不失效。
     *
     * <p>文件格式：魔数 + 版本 + 条目数，每条目为「键（SHA-256 十六进制）+ 最近使用时间 +
     * 权重数组 + 修正胜率数组」。采用最近最少使用（LRU）上限 {@value #MAX_ENTRIES} 份，
     * 超出自动删除最久未用的条目；文件损坏或版本不符时按空缓存处理，不影响分析。
     * 缓存值与原始阵型无关，可跨分析复用。
     */
    private static final class StrengthCache {

        private static final String FILE_NAME = "strength_cache.bin";
        private static final int MAGIC = 0x474B5443;
        private static final int VERSION = 3;
        private static final int MAX_ENTRIES = 32;
        private static final int MAX_POOL_SIZE = 1_000_000;

        private final Object lock = new Object();
        private LinkedHashMap<String, Entry> entries;
        private boolean loaded;

        private record Entry(long lastUsed, PoolRating rating) {}

        /** 命中时更新最近使用时间并落盘；返回副本。 */
        PoolRating get(String key) {
            synchronized (lock) {
                ensureLoaded();
                Entry entry = entries.get(key);
                if (entry == null) {
                    return null;
                }
                entries.put(key, new Entry(System.currentTimeMillis(), entry.rating()));
                save();
                return copy(entry.rating());
            }
        }

        void put(String key, PoolRating rating) {
            synchronized (lock) {
                ensureLoaded();
                entries.put(key, new Entry(System.currentTimeMillis(), copy(rating)));
                while (entries.size() > MAX_ENTRIES) {
                    Iterator<String> it = entries.keySet().iterator();
                    it.next();
                    it.remove();
                }
                save();
            }
        }

        private static PoolRating copy(PoolRating rating) {
            return new PoolRating(rating.weights().clone(), rating.rates().clone());
        }

        private void ensureLoaded() {
            if (loaded) {
                return;
            }
            loaded = true;
            entries = new LinkedHashMap<>(16, 0.75f, true);
            Path path = Paths.get(FILE_NAME);
            if (!Files.isRegularFile(path)) {
                return;
            }
            try (DataInputStream in = new DataInputStream(
                    new BufferedInputStream(Files.newInputStream(path)))) {
                if (in.readInt() != MAGIC || in.readInt() != VERSION) {
                    return;
                }
                int count = in.readInt();
                if (count < 0 || count > 4096) {
                    return;
                }
                List<Map.Entry<String, Entry>> list = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    String key = in.readUTF();
                    long lastUsed = in.readLong();
                    int len = in.readInt();
                    if (len < 0 || len > MAX_POOL_SIZE) {
                        return;
                    }
                    double[] weights = new double[len];
                    double[] rates = new double[len];
                    for (int j = 0; j < len; j++) {
                        weights[j] = in.readDouble();
                    }
                    for (int j = 0; j < len; j++) {
                        rates[j] = in.readDouble();
                    }
                    list.add(Map.entry(key, new Entry(lastUsed, new PoolRating(weights, rates))));
                }
                // 按最近使用时间升序重排，使链式哈希表的迭代顺序即 LRU 顺序
                list.sort(Comparator.comparingLong(e -> e.getValue().lastUsed()));
                for (Map.Entry<String, Entry> e : list) {
                    entries.put(e.getKey(), e.getValue());
                }
                while (entries.size() > MAX_ENTRIES) {
                    Iterator<String> it = entries.keySet().iterator();
                    it.next();
                    it.remove();
                }
            } catch (IOException | RuntimeException ex) {
                entries = new LinkedHashMap<>(16, 0.75f, true);
            }
        }

        private void save() {
            Path path = Paths.get(FILE_NAME);
            Path tmp = Paths.get(FILE_NAME + ".tmp");
            try (DataOutputStream out = new DataOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(tmp)))) {
                out.writeInt(MAGIC);
                out.writeInt(VERSION);
                out.writeInt(entries.size());
                for (Map.Entry<String, Entry> e : entries.entrySet()) {
                    out.writeUTF(e.getKey());
                    out.writeLong(e.getValue().lastUsed());
                    double[] weights = e.getValue().rating().weights();
                    double[] rates = e.getValue().rating().rates();
                    out.writeInt(weights.length);
                    for (double v : weights) {
                        out.writeDouble(v);
                    }
                    for (double v : rates) {
                        out.writeDouble(v);
                    }
                }
            } catch (IOException ex) {
                return;
            }
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // 落盘失败不影响分析，下次访问再试
            }
        }
    }

    private static double winRate(int win, int lose, int draw) {
        int decisive = win + lose + draw;
        return decisive > 0 ? (2.0 * win + draw) * 50.0 / decisive : Double.NaN;
    }

    /** 胜率向 50% 收缩：样本越少越接近 50%（伪计数 SHRINK_PRIOR 局）。 */
    private static double shrink(double rate, int decisive) {
        if (Double.isNaN(rate) || decisive <= 0) {
            return Double.NaN;
        }
        return (rate * decisive + 50.0 * SHRINK_PRIOR) / (decisive + SHRINK_PRIOR);
    }
}
