package org.example;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {
    public static int MAX_FRAME_LIMIT = 65536;    //最大运行帧数
    public static boolean SHOW_REMAIN_HP = false;
    public static int MAX_THREADS = Runtime.getRuntime().availableProcessors();
    public static final String CONFIG_FILE = "config.ini";

    public static String pskey = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";      //密码表
    private static final Scanner scanner = new Scanner(System.in);
    public static List<CompiledFort> p1List;
    public static List<CompiledFort> p2List;
    public static ExecutorService pool;

    public static void main(String[] args) {
        if (java.util.Arrays.asList(args).contains("--cli")) {
            runCli();
        } else {
            Setting.loadConfig();
            pool = Executors.newFixedThreadPool(MAX_THREADS);
            javax.swing.SwingUtilities.invokeLater(() -> {
                try {
                    new MainGUI().setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private static void runCli() {
        Setting.loadConfig();
        pool = Executors.newFixedThreadPool(MAX_THREADS);
        System.out.println("正在导入阵容...");
        p1List = Setting.CompileForts("1P.txt");
        p2List = Setting.CompileForts("2P.txt");
        System.out.println("阵容导入完成！");
        while (Setting.setting(scanner)) {
            runAllBattles(null);
        }
        pool.shutdown();
    }

    public static java.util.List<FortStats> runAllBattles(java.util.function.Consumer<Integer> onProgress) {
        long total_start = System.nanoTime();

        java.util.List<java.util.concurrent.Future<Result>> futures = new java.util.ArrayList<>();
        java.util.List<String> meta = new java.util.ArrayList<>();
        for (int j = 0; j < p1List.size(); j++) {
            for (int i = 0; i < p2List.size(); i++) {
                CompiledFort f1 = p1List.get(j);
                CompiledFort f2 = p2List.get(i);

                int roundIndex = j * 200 + i + 1;

                futures.add(pool.submit(() -> {
                    GameTask g = new GameTask();
                    return g.run_single(f1, f2);
                }));

                meta.add("Round " + roundIndex +
                        " | 1P[" + f1.name + "] vs 2P[" + f2.name + "]");
            }
        }

        StringBuilder final_result = new StringBuilder();
        StringBuilder simple_result = new StringBuilder();
        java.util.List<FortStats> statsList = new java.util.ArrayList<>();
        int done = 0;
        int score = 0;
        int win = 0;
        int lose = 0;
        int draw = 0;
        int unknown = 0;
        for (int i = 0; i < futures.size(); i++) {
            try {
                Result r = futures.get(i).get();
                final_result.append(meta.get(i)).append("\n");
                String resultStr = switch (r.status) {
                    case 1 -> "1P 获胜";
                    case 2 -> "2P 获胜";
                    case 0 -> "平局";
                    case -1 -> "超时";
                    default -> "异常";
                };
                final_result.append("结果: ").append(resultStr)
                        .append(" | 剩余血量: ").append(r.winnerHp)
                        .append(" | 总帧数: ").append(r.framePassed)
                        .append(" | 用时: ").append(String.format("%.3f ms", r.timeUsed))
                        .append("\n\n");
                if (i % p2List.size() == 0) {
                    if (i > 0) {
                        simple_result.append("\n\n");
                        statsList.add(new FortStats(
                                p1List.get((i / p2List.size()) - 1).name,
                                win, lose, draw, unknown, score, p2List.size()
                        ));
                        score = 0;
                        win = 0;
                        lose = 0;
                        draw = 0;
                        unknown = 0;
                    }
                    simple_result.append(p1List.get(i / p2List.size()).name).append(": \n");
                }
                simple_result.append(r.getSimpleResult());
                score += r.getScore();
                win += r.status == 1 ? 1 : 0;
                lose += r.status == 2 ? 1 : 0;
                draw += r.status == 0 ? 1 : 0;
                unknown += r.status < 0 ? 1 : 0;
                if ((i + 1) % p2List.size() == 0) {
                    simple_result.append("\n")
                            .append("总场次: %d".formatted(p2List.size()))
                            .append(", 胜: %d".formatted(win))
                            .append(", 负: %d".formatted(lose))
                            .append(", 平: %d".formatted(draw))
                            .append(", 未定: %d".formatted(unknown))
                            .append(", 胜率: %.2f".formatted((2 * win + draw) * 50F / (win + lose + draw)))
                            .append("%, 血量积分: ").append(score);
                }
                done++;
                if (onProgress != null) {
                    onProgress.accept(done);
                }
                System.out.print("\r进度: " + done + "/" + meta.size());
            } catch (Exception e) {
                Throwable cause = e.getCause();

                Objects.requireNonNullElse(cause, e).printStackTrace();
                final_result.append(meta.get(i))
                        .append("\nERROR: ")
                        .append(cause != null ? cause : e)
                        .append("\n\n");
            }
        }
        // Add last fort stats
        if (!p1List.isEmpty()) {
            statsList.add(new FortStats(
                    p1List.get(p1List.size() - 1).name,
                    win, lose, draw, unknown, score, p2List.size()
            ));
        }
        Setting.writeResult("simple_result.txt", simple_result.toString());
        float total_time = (System.nanoTime() - total_start) / 1000000.F;
        final_result.append("====SUMMARY====\n总轮次: ").append(meta.size()).append(String.format("\n总用时: %.3f ms", total_time));
        Setting.writeResult("result.txt", final_result.toString());
        System.out.printf("%n所有对局已完成%n总用时: %.3f ms%n", total_time);
        return statsList;
    }

    public static class FortStats {
        public final String name;
        public final int win, lose, draw, unknown, total;
        public final int score;

        public FortStats(String name, int win, int lose, int draw, int unknown, int score, int total) {
            this.name = name;
            this.win = win;
            this.lose = lose;
            this.draw = draw;
            this.unknown = unknown;
            this.score = score;
            this.total = total;
        }

        public double winRate() {
            int decisive = win + lose + draw;
            return decisive > 0 ? (2.0 * win + draw) * 50.0 / decisive : 0;
        }
    }

    public static CompiledFort compileFort(Fort f) {
        String code = f.code();
        if (code.length() < 6) {
            throw new IllegalArgumentException("阵容代码长度不足: " + f.name() + " [" + code + "]");
        }

        int[] core = to_xyr(code.substring(1, 6));
        int baseSeed = (core[0] % 168 + 48) * (core[1] % 168 + 48);
        int coreX = core[0] - 190;
        int coreY = core[1] - 400;
        int coreType = code.charAt(0) - '0';
        int unitCount = code.length() / 6 - 1;
        int[] type = new int[unitCount];
        int[] x = new int[unitCount];
        int[] y = new int[unitCount];
        int[] r = new int[unitCount];
        int[] seed = new int[unitCount];

        for (int i = 0, j = 6; i < unitCount; i++, j += 6) {
            int[] u = to_xyr(code.substring(j + 1, j + 6));
            int t = Main.pskey.indexOf(code.charAt(j));
            type[i] = t;
            x[i] = u[0];
            y[i] = u[1];
            r[i] = u[2];
            seed[i] = baseSeed * (u[0] % 185 + 30) * (u[1] % 185 + 30);
        }
        return new CompiledFort(
                f.name(),
                coreType, coreX, coreY,
                baseSeed,
                unitCount,
                type, x, y, r, seed
        );
    }

    public static int[] to_xyr(String str){    //5位61进制转x y r数组
        int rxy = 0;
        rxy += Main.pskey.indexOf(str.charAt(0)) * 13845841;
        rxy += Main.pskey.indexOf(str.charAt(1)) * 226981;
        rxy += Main.pskey.indexOf(str.charAt(2)) * 3721;
        rxy += Main.pskey.indexOf(str.charAt(3)) * 61;
        rxy += Main.pskey.indexOf(str.charAt(4));
        int r = rxy / 1_000_000;
        int x = (rxy / 1_000) % 1_000;
        int y = rxy % 1_000;
        return new int[]{x, y, r};
    }
}