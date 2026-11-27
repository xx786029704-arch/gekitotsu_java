package org.example.tools;

import org.example.CompiledFort;
import org.example.Fort;
import org.example.GameTask;
import org.example.Main;
import org.example.Result;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * 只读基准：与 Rust `gkt-bench` 同口径（预编译阵型复用、warmup + repeat、P50/P95）。
 *
 * <p>用法：{@code java ... org.example.tools.Bench <cases.txt> [warmup] [repeat]}。
 * 每局耗时含引擎内部计时开销，timeUsed 非确定但量级可忽略。不修改引擎任何行为。
 */
public final class Bench {

    private Bench() {
    }

    public static void main(String[] args) throws Exception {
        String casesPath = args[0];
        int warmup = args.length > 1 ? Integer.parseInt(args[1]) : 1;
        int repeat = args.length > 2 ? Integer.parseInt(args[2]) : 3;
        int threads = args.length > 3 ? Integer.parseInt(args[3]) : 1;

        List<String[]> cases = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of(casesPath), StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            cases.add(t.split("\\s+"));
        }
        Map<String, CompiledFort> cache = new HashMap<>();
        for (String[] p : cases) {
            cache.computeIfAbsent(p[0], c -> Main.compileFort(new Fort("", c)));
            cache.computeIfAbsent(p[1], c -> Main.compileFort(new Fort("", c)));
        }

        System.out.println("cases=" + cases.size() + " unique_forts=" + cache.size()
                + " warmup=" + warmup + " repeat=" + repeat + " threads=" + threads);

        if (threads <= 1) {
            runSingleThreaded(cases, cache, warmup, repeat);
        } else {
            runMultiThreaded(cases, cache, warmup, repeat, threads);
        }
    }

    private static void runSingleThreaded(
            List<String[]> cases, Map<String, CompiledFort> cache, int warmup, int repeat) {
        for (int w = 0; w < warmup; w++) {
            for (String[] p : cases) {
                run(cache, p);
            }
        }

        double[] samples = new double[cases.size() * repeat];
        int index = 0;
        long totalFrames = 0;
        double totalSecs = 0.0;
        long wallStart = System.nanoTime();
        for (int r = 0; r < repeat; r++) {
            for (String[] p : cases) {
                long start = System.nanoTime();
                Result result = run(cache, p);
                double elapsed = (System.nanoTime() - start) / 1e9;
                samples[index++] = elapsed;
                totalSecs += elapsed;
                totalFrames += result.framePassed;
            }
        }
        double wall = (System.nanoTime() - wallStart) / 1e9;
        Arrays.sort(samples);
        System.out.printf(
                "sum: games=%d frames=%d cpu_secs=%.3f wall_secs=%.3f games/s=%.2f frames/s=%.0f%n",
                samples.length, totalFrames, totalSecs, wall,
                samples.length / wall, totalFrames / wall);
        System.out.printf(
                "per_game_ms: mean=%.3f p50=%.3f p95=%.3f max=%.3f%n",
                totalSecs / samples.length * 1000,
                percentile(samples, 50) * 1000,
                percentile(samples, 95) * 1000,
                samples[samples.length - 1] * 1000);
    }

    private static void runMultiThreaded(
            List<String[]> cases,
            Map<String, CompiledFort> cache,
            int warmup,
            int repeat,
            int threads
    ) {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int w = 0; w < warmup; w++) {
                runPass(cases, cache, pool);
            }
            long totalFrames = 0;
            long wallStart = System.nanoTime();
            for (int r = 0; r < repeat; r++) {
                totalFrames += runPass(cases, cache, pool);
            }
            double wall = (System.nanoTime() - wallStart) / 1e9;
            long games = (long) cases.size() * repeat;
            System.out.printf(
                    "sum: games=%d frames=%d cpu_secs=%.3f wall_secs=%.3f games/s=%.2f frames/s=%.0f%n",
                    games, totalFrames, 0.0, wall, games / wall, totalFrames / wall);
        } finally {
            pool.shutdownNow();
        }
    }

    private static long runPass(
            List<String[]> cases, Map<String, CompiledFort> cache, ExecutorService pool) {
        List<Future<Long>> futures = new ArrayList<>(cases.size());
        for (String[] p : cases) {
            futures.add(pool.submit(() -> (long) run(cache, p).framePassed));
        }
        long frames = 0;
        for (Future<Long> future : futures) {
            try {
                frames += future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return frames;
    }

    private static Result run(Map<String, CompiledFort> cache, String[] p) {
        Integer seed = "-".equals(p[2]) ? null : Integer.valueOf(p[2]);
        GameTask game = new GameTask();
        return game.run_single(
                cache.get(p[0]), cache.get(p[1]), Integer.parseInt(p[3]), seed, null);
    }

    private static double percentile(double[] sorted, double p) {
        int rank = (int) Math.ceil(p / 100.0 * sorted.length);
        return sorted[Math.max(0, Math.min(sorted.length - 1, rank - 1))];
    }
}
