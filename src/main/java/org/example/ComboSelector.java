package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** 删除组合的枚举与随机采样。 */
public final class ComboSelector {

    /** 单次分析的组合数硬上限，超过时拒绝（防止内存耗尽）。 */
    public static final long MAX_SAMPLES = 1_000_000L;

    private ComboSelector() {}

    /** C(n, k)，参数非法返回 0；用 double 中间值避免 long 溢出，结果在 2^53 以内精确，更大时为近似值或饱和。 */
    public static long combinationCount(int n, int k) {
        if (n < 0 || k < 0 || k > n) {
            return 0;
        }
        if (k == 0 || k == n) {
            return 1;
        }
        k = Math.min(k, n - k);
        double result = 1;
        for (int i = 1; i <= k; i++) {
            result = result * (n - k + i) / i;
            if (result > 9.22e18) {
                return Long.MAX_VALUE;
            }
        }
        return Math.round(result);
    }

    /** 按探索率计算采样数（不含全遍历判定）。0% 返回 0；非 0% 至少 1 个。 */
    public static long plannedCount(long total, int explorationPercent) {
        int m = Math.max(0, Math.min(100, explorationPercent));
        if (m == 0 || total <= 0) {
            return 0;
        }
        if (m == 100 || total == 1) {
            return total;
        }
        return Math.min(total, Math.max(1L, Math.round(total * (m / 100.0))));
    }

    /** 组合的字典序排名；comb 需严格升序。 */
    public static long rank(int[] comb, int n) {
        long rank = 0;
        int start = 0;
        for (int i = 0; i < comb.length; i++) {
            for (int c = start; c < comb[i]; c++) {
                rank += combinationCount(n - c - 1, comb.length - i - 1);
            }
            start = comb[i] + 1;
        }
        return rank;
    }

    /** 字典序排名反解为组合；要求 0 <= rank < C(n,k)。 */
    public static int[] unrank(long rank, int n, int k) {
        if (k < 0 || k > n || rank < 0 || rank >= combinationCount(n, k)) {
            throw new IllegalArgumentException(I18n.t("combo.err.rankOob", rank, n, k));
        }
        int[] comb = new int[k];
        int start = 0;
        for (int i = 0; i < k; i++) {
            long count;
            while ((count = combinationCount(n - start - 1, k - i - 1)) <= rank) {
                rank -= count;
                start++;
            }
            comb[i] = start;
            start++;
        }
        return comb;
    }

    /** 采样方案：ranked=true 时 ranks 为按字典序排名的组合；否则按字典序全遍历。 */
    public record Selection(int unitCount, int deleteCount, long total, int count,
                            long[] ranks, boolean ranked) {

        /** 展开组合列表。 */
        public List<int[]> materialize() {
            if (!ranked) {
                if (count > MAX_SAMPLES) {
                    throw new IllegalStateException(I18n.t("combo.err.tooLarge"));
                }
                return enumerate(unitCount, deleteCount, count);
            }
            List<int[]> list = new ArrayList<>(ranks.length);
            for (long r : ranks) {
                list.add(unrank(r, unitCount, deleteCount));
            }
            return list;
        }
    }

    /** 生成采样方案。探索率 100% 或采样数达到总数时为全遍历。 */
    public static Selection plan(int unitCount, int deleteCount, int explorationPercent, Random rng) {
        if (unitCount < 1) {
            throw new IllegalArgumentException(I18n.t("combo.err.none"));
        }
        if (deleteCount < 1 || deleteCount > unitCount) {
            throw new IllegalArgumentException(I18n.t("combo.err.range", unitCount));
        }
        long total = combinationCount(unitCount, deleteCount);
        long k = plannedCount(total, explorationPercent);
        if (k > MAX_SAMPLES) {
            throw new IllegalArgumentException(I18n.t("combo.err.overCap", MAX_SAMPLES));
        }
        if (k <= 0) {
            return new Selection(unitCount, deleteCount, total, 0, new long[0], true);
        }
        if (k >= total) {
            return new Selection(unitCount, deleteCount, total,
                    (int) Math.min(total, Integer.MAX_VALUE), new long[0], false);
        }
        int kInt = (int) Math.min(k, Integer.MAX_VALUE);
        int capacity = (int) Math.min((long) kInt * 2, 1 << 20);
        Set<Long> chosen = new HashSet<>(capacity);
        for (long j = total - kInt; j < total; j++) {
            long t = Math.floorMod(rng.nextLong(), j + 1);
            if (!chosen.add(t)) {
                chosen.add(j);
            }
        }
        long[] ranks = new long[chosen.size()];
        int idx = 0;
        for (long r : chosen) {
            ranks[idx++] = r;
        }
        Arrays.sort(ranks);
        return new Selection(unitCount, deleteCount, total, ranks.length, ranks, true);
    }

    private static List<int[]> enumerate(int n, int k, int limit) {
        List<int[]> list = new ArrayList<>(Math.min(limit, 1 << 16));
        int[] comb = new int[k];
        for (int i = 0; i < k; i++) {
            comb[i] = i;
        }
        while (list.size() < limit) {
            list.add(comb.clone());
            int i = k - 1;
            while (i >= 0 && comb[i] == n - k + i) {
                i--;
            }
            if (i < 0) {
                break;
            }
            comb[i]++;
            for (int j = i + 1; j < k; j++) {
                comb[j] = comb[j - 1] + 1;
            }
        }
        return list;
    }
}
