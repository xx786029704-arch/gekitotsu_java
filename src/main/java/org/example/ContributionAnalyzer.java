package org.example;

import org.example.GUI.Formation;
import org.example.GUI.Unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.BooleanSupplier;

/** 贡献分析：批量对战计算“删除 n 个单位”前后的胜率变化。 */
public final class ContributionAnalyzer {

    public enum SortKey { DELTA, PER_COST }

    public enum SortOrder { DESC, ASC }

    public record Params(int deleteCount, int explorationPercent, int threads,
                         SortKey sortKey, SortOrder order) {}

    public record UnitRef(int index, int type, int x, int y, int cost, String name) {}

    public record ComboResult(int[] unitIndices, long removedCost,
                              int win, int lose, int draw, int timeout,
                              double winRate, double delta, double deltaPerCost) {}

    public record UnitSummary(UnitRef unit, int appearances,
                              double avgDelta, double avgDeltaPerCost) {}

    public record Baseline(int win, int lose, int draw, int timeout, double winRate) {}

    public record Report(int unitCount, long totalCost, List<UnitRef> refs, Baseline baseline,
                         int sampledCombos, long totalCombos,
                         List<ComboResult> combos, List<UnitSummary> units,
                         int battleErrors, long elapsedMs) {}

    /** 进度回调。注意：回调在 analyze 的调用线程（后台分析线程）触发，不是 EDT，GUI 侧需自行切换线程。 */
    public interface ProgressListener {
        void onProgress(int doneBattles, long totalBattles, String phase);

        default void onBaseline(Baseline baseline) {
        }
    }

    private ContributionAnalyzer() {}

    /**
     * 执行贡献分析。
     *
     * @return 分析结果；取消（cancelled 为 true 或线程中断）时返回 null
     * @throws Exception 输入解析/参数错误，或对战任务抛出异常时
     */
    public static Report analyze(String playerText, String evalText, Params params,
                                 ProgressListener listener, BooleanSupplier cancelled) throws Exception {
        long startNanos = System.nanoTime();

        List<CompiledFort> playerForts = Setting.parseForts(playerText);
        if (playerForts.size() != 1) {
            throw new IllegalArgumentException("玩家阵型必须且只能包含一个阵型");
        }
        CompiledFort player = playerForts.get(0);
        Formation formation = Formation.decode(playerText.trim());
        int unitCount = player.unitCount;
        if (formation.units.size() - 1 != unitCount) {
            throw new IllegalArgumentException("阵型文本与代码解析结果不一致");
        }

        List<CompiledFort> evalForts = Setting.parseForts(evalText);
        if (evalForts.isEmpty()) {
            throw new IllegalArgumentException("评判阵集为空或格式错误");
        }

        int n = params.deleteCount();
        if (n < 1 || n > unitCount) {
            throw new IllegalArgumentException("删除数必须在 1~" + unitCount + " 之间");
        }

        List<UnitRef> refs = new ArrayList<>();
        long totalCost = 0;
        for (int i = 0; i < unitCount; i++) {
            int type = player.type[i];
            Unit.Info info = (type >= 0 && type < Unit.infos.length)
                    ? Unit.infos[type] : Unit.infos[Unit.infos.length - 1];
            int cost = info.cost() > 0 ? info.cost() : 1;
            refs.add(new UnitRef(i, type, player.x[i], player.y[i], cost, info.name()));
            totalCost += cost;
        }

        ComboSelector.Selection selection =
                ComboSelector.plan(unitCount, n, params.explorationPercent(), new Random());
        List<int[]> combos = selection.materialize();
        int k = combos.size();
        int m = evalForts.size();
        int threads = Math.max(1, Math.min(256, params.threads()));
        long totalBattles = (long) (k + 1) * m;
        int window = Math.max(64, threads * 4);

        int[] baselineCounts = new int[4];
        int[] comboCounts = new int[k * 4];
        Map<Integer, CompiledFort> activeForts = new HashMap<>();
        int[] pendingBattles = new int[k];
        int errors = 0;
        int battlesDone = 0;
        int baselineDone = 0;
        boolean baselineReported = false;
        long lastNotify = 0;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CompletionService<int[]> completion = new ExecutorCompletionService<>(pool);
            int next = 0;
            int inFlight = 0;
            while ((next < totalBattles || inFlight > 0) && !cancelled.getAsBoolean()) {
                while (inFlight < window && next < totalBattles) {
                    int job = next++;
                    int comboIdx = job < m ? -1 : (job - m) / m;
                    int oppIdx = job < m ? job : (job - m) % m;
                    // 惰性裁剪：只保留窗口内仍有在途对战的组合，避免一次性持有全部裁剪结果
                    final CompiledFort p1;
                    if (comboIdx < 0) {
                        p1 = player;
                    } else {
                        CompiledFort active = activeForts.get(comboIdx);
                        if (active == null) {
                            active = FortTrimmer.trim(player, combos.get(comboIdx));
                            activeForts.put(comboIdx, active);
                        }
                        p1 = active;
                        pendingBattles[comboIdx]++;
                    }
                    CompiledFort p2 = evalForts.get(oppIdx);
                    completion.submit(() -> {
                        try {
                            Result r = new GameTask().run_single(p1, p2);
                            return new int[]{comboIdx, r.status};
                        } catch (Exception t) {
                            return new int[]{comboIdx, -2};
                        }
                    });
                    inFlight++;
                }
                if (inFlight == 0) {
                    break;
                }
                Future<int[]> future = completion.take();
                int[] res = future.get();
                inFlight--;
                battlesDone++;
                int bucket = bucketOf(res[1]);
                if (res[1] == -2) {
                    errors++;
                }
                if (res[0] < 0) {
                    baselineCounts[bucket]++;
                    baselineDone++;
                    if (!baselineReported && baselineDone == m) {
                        baselineReported = true;
                        if (listener != null) {
                            listener.onBaseline(toBaseline(baselineCounts));
                        }
                    }
                } else {
                    comboCounts[res[0] * 4 + bucket]++;
                    if (--pendingBattles[res[0]] == 0) {
                        activeForts.remove(res[0]);
                    }
                }
                long now = System.nanoTime();
                if (listener != null && (now - lastNotify > 100_000_000L || battlesDone == totalBattles)) {
                    lastNotify = now;
                    listener.onProgress(battlesDone, totalBattles, res[0] < 0 ? "基线" : "组合");
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } finally {
            // 已提交的对战跑完即止（GameTask 不响应中断）
            pool.shutdownNow();
        }

        if (cancelled.getAsBoolean() || baselineDone < m) {
            return null;
        }

        Baseline baseline = toBaseline(baselineCounts);
        double baseRate = baseline.winRate();

        List<ComboResult> comboResults = new ArrayList<>(k);
        for (int ci = 0; ci < k; ci++) {
            int base = ci * 4;
            int win = comboCounts[base];
            int lose = comboCounts[base + 1];
            int draw = comboCounts[base + 2];
            int timeout = comboCounts[base + 3];
            long removedCost = 0;
            for (int idx : combos.get(ci)) {
                removedCost += refs.get(idx).cost();
            }
            double comboRate = winRate(win, lose, draw);
            double delta = comboRate - baseRate;
            comboResults.add(new ComboResult(combos.get(ci), removedCost,
                    win, lose, draw, timeout, comboRate, delta, delta / removedCost));
        }
        sortCombos(comboResults, params.sortKey(), params.order());

        double[] sumDelta = new double[unitCount];
        double[] sumDeltaPerCost = new double[unitCount];
        int[] appearances = new int[unitCount];
        for (ComboResult cr : comboResults) {
            for (int idx : cr.unitIndices()) {
                sumDelta[idx] += cr.delta();
                sumDeltaPerCost[idx] += cr.deltaPerCost();
                appearances[idx]++;
            }
        }
        List<UnitSummary> unitSummaries = new ArrayList<>();
        for (int i = 0; i < unitCount; i++) {
            if (appearances[i] == 0) {
                continue;
            }
            double avgDelta = sumDelta[i] / appearances[i];
            // 单位每费变化 = 该单位所有被采样组合的「组合每费变化」平均值（而非 avgDelta/自身cost）
            unitSummaries.add(new UnitSummary(refs.get(i), appearances[i],
                    avgDelta, sumDeltaPerCost[i] / appearances[i]));
        }
        sortUnits(unitSummaries, params.sortKey(), params.order());

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000L;
        return new Report(unitCount, totalCost, refs, baseline, k, selection.total(),
                comboResults, unitSummaries, errors, elapsedMs);
    }

    /** 生成删除指定单位后的阵型代码（name&code#hp），索引 0 对应第一个非核心单位。 */
    public static String removeUnitsFromCode(String playerText, int[] unitIndices) {
        Formation formation = Formation.decode(playerText.trim());
        boolean[] remove = new boolean[formation.units.size()];
        for (int idx : unitIndices) {
            if (idx < 0 || idx >= formation.units.size() - 1) {
                throw new IllegalArgumentException("单位索引越界: " + idx);
            }
            remove[idx + 1] = true;
        }
        List<Unit> kept = new ArrayList<>();
        for (int i = 0; i < formation.units.size(); i++) {
            if (!remove[i]) {
                kept.add(formation.units.get(i));
            }
        }
        Formation result = new Formation(formation.name, kept);
        String encoded = result.encode();
        if (formation.name == null || formation.name.isEmpty()) {
            return encoded + result.encodeHp();
        }
        return encoded;
    }

    private static int bucketOf(int status) {
        return status == 1 ? 0 : status == 2 ? 1 : status == 0 ? 2 : 3;
    }

    private static double winRate(int win, int lose, int draw) {
        int decisive = win + lose + draw;
        return decisive > 0 ? (2.0 * win + draw) * 50.0 / decisive : Double.NaN;
    }

    private static Baseline toBaseline(int[] counts) {
        return new Baseline(counts[0], counts[1], counts[2], counts[3],
                winRate(counts[0], counts[1], counts[2]));
    }

    private static void sortCombos(List<ComboResult> list, SortKey key, SortOrder order) {
        Comparator<ComboResult> cmp = (a, b) -> compareValues(
                key == SortKey.DELTA ? a.delta() : a.deltaPerCost(),
                key == SortKey.DELTA ? b.delta() : b.deltaPerCost(),
                order);
        list.sort(cmp.thenComparing(ComboResult::unitIndices, Arrays::compare));
    }

    private static void sortUnits(List<UnitSummary> list, SortKey key, SortOrder order) {
        Comparator<UnitSummary> cmp = (a, b) -> compareValues(
                key == SortKey.DELTA ? a.avgDelta() : a.avgDeltaPerCost(),
                key == SortKey.DELTA ? b.avgDelta() : b.avgDeltaPerCost(),
                order);
        list.sort(cmp.thenComparingInt(s -> s.unit().index()));
    }

    /** NaN 永远排在最后；其他值按指定方向比较。 */
    private static int compareValues(double a, double b, SortOrder order) {
        boolean na = Double.isNaN(a);
        boolean nb = Double.isNaN(b);
        if (na || nb) {
            return na == nb ? 0 : (na ? 1 : -1);
        }
        int c = Double.compare(b, a);
        return order == SortOrder.DESC ? c : -c;
    }
}
