package org.example;

import org.example.GUI.FormulaTable;
import org.example.GUI.MainGUI;
import org.example.GUI.SplashWindow;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static final String VERSION = Version.VERSION;
    public static int MAX_FRAME_LIMIT = 65536;    //最大运行帧数
    public static boolean SHOW_REMAIN_HP = false;
    public static boolean WORD_WRAP = false;
    public static boolean DARK_MODE = true;
    public static String ACCENT_COLOR = "#2675BF";
    public static String LANGUAGE = I18n.detectSystemLang();   //zh/ja/en
    public static int MAX_THREADS = Runtime.getRuntime().availableProcessors();
    public static final String CONFIG_FILE = "config.ini";

    public static String pskey = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";      //密码表
    private static final Scanner scanner = new Scanner(System.in);
    public static List<CompiledFort> p1List;
    public static List<CompiledFort> p2List;
    public static ExecutorService pool;
    public static FormulaTable formulaTable;

    public static void main(String[] args) {
        if (java.util.Arrays.asList(args).contains("--cli")) {
            runCli();
        } else {
            Setting.loadConfig();
            applyTheme(DARK_MODE, ACCENT_COLOR);
            SplashWindow splash = SplashWindow.createAndShow();
            try {
                pool = Executors.newFixedThreadPool(MAX_THREADS);
                if (splash != null) splash.setStatus(I18n.t("splash.loadingFormula"));
                formulaTable = new FormulaTable();
                if (splash != null) splash.setStatus(I18n.t("splash.loadingUi"));
                javax.swing.SwingUtilities.invokeLater(() -> {
                    try {
                        MainGUI gui = new MainGUI();
                        gui.addWindowListener(new java.awt.event.WindowAdapter() {
                            @Override
                            public void windowOpened(java.awt.event.WindowEvent e) {
                                if (splash != null) splash.close();
                            }
                        });
                        gui.setVisible(true);
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (splash != null) splash.closeImmediately();
                    }
                });
            } catch (Exception e) {
                if (splash != null) splash.closeImmediately();
                throw new RuntimeException(e);
            }
        }
    }

    private static void runCli() {
        Setting.loadConfig();
        pool = Executors.newFixedThreadPool(MAX_THREADS);
        System.out.println(I18n.t("main.importing"));
        p1List = Setting.CompileForts("1P.txt");
        p2List = Setting.CompileForts("2P.txt");
        System.out.println(I18n.t("main.importDone"));
        while (Setting.setting(scanner)) {
            runAllBattles(null);
        }
        pool.shutdown();
    }

    public static void applyTheme(boolean dark, String accentHex) {
        if (accentHex != null && !accentHex.isEmpty()) {
            System.setProperty("flatlaf.accentColor", accentHex);
            java.util.Map<String, String> extras = new java.util.HashMap<>();
            extras.put("@accentColor", accentHex);
            com.formdev.flatlaf.FlatLaf.setGlobalExtraDefaults(extras);
        }
        if (dark) {
            com.formdev.flatlaf.FlatDarkLaf.setup();
        } else {
            com.formdev.flatlaf.FlatLightLaf.setup();
        }
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

                meta.add(I18n.t("result.round", roundIndex, f1.name, f2.name));
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
                    case 1 -> I18n.t("result.p1win");
                    case 2 -> I18n.t("result.p2win");
                    case 0 -> I18n.t("result.draw");
                    case -1 -> I18n.t("result.timeout");
                    default -> I18n.t("result.abnormal");
                };
                final_result.append(I18n.t("result.line", resultStr, r.winnerHp, r.framePassed,
                                String.format("%.3f", r.timeUsed)))
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
                            .append(I18n.t("result.stats", p2List.size(), win, lose, draw, unknown,
                                    (2 * win + draw) * 50F / (win + lose + draw), score));
                }
                done++;
                if (onProgress != null) {
                    onProgress.accept(done);
                }
                System.out.print("\r" + I18n.t("result.progress", done, meta.size()));
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
        final_result.append(I18n.t("result.summary", meta.size(), String.format("%.3f", total_time)));
        Setting.writeResult("result.txt", final_result.toString());
        System.out.printf("%n" + I18n.t("main.allDone") + "%n", total_time);
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
            throw new IllegalArgumentException(I18n.t("err.codeTooShort", f.name(), code));
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