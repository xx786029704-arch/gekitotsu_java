package org.example.training;

import org.example.Utils;

import java.util.stream.IntStream;

/**
 * 从一个主种子生成稳定的对局种子序列。
 *
 * <p>比较不同候选阵型时应复用同一组 episode 种子，以减少随机噪声。</p>
 */
public final class SeedSchedule {
    private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

    private SeedSchedule() {
    }

    public static int forEpisode(long masterSeed, long episodeIndex) {
        if (episodeIndex < 0) {
            throw new IllegalArgumentException("episodeIndex 不能为负数");
        }
        long z = masterSeed + GOLDEN_GAMMA * (episodeIndex + 1);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return Utils.normalizeSeed(z);
    }

    public static int[] create(long masterSeed, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count 不能为负数");
        }
        return IntStream.range(0, count)
                .map(index -> forEpisode(masterSeed, index))
                .toArray();
    }
}
