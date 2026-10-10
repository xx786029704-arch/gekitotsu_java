package org.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.Executors;

public class Setting {
    public Setting(){
        return;
    }

    public static boolean setting(Scanner scanner){
        while (true){
            System.out.println("-==== " + I18n.t("cli.menuTitle") + " ====-");
            System.out.println(I18n.t("cli.choose"));
            System.out.println(I18n.t("cli.item0"));
            System.out.println(I18n.t("cli.item1", Main.MAX_FRAME_LIMIT));
            System.out.println(Main.SHOW_REMAIN_HP ? I18n.t("cli.item2off") : I18n.t("cli.item2on"));
            System.out.println(I18n.t("cli.item3"));
            System.out.println(I18n.t("cli.item4", Main.MAX_THREADS));
            System.out.println(I18n.t("cli.item9"));
            System.out.println(I18n.t("cli.inputHint"));
            switch (scanner.nextLine()){
                case "0":{
                    return true;
                }
                case "1":{
                    System.out.print(I18n.t("cli.setFramePrompt"));
                    Main.MAX_FRAME_LIMIT = Integer.parseInt(scanner.nextLine());
                    if (Main.MAX_FRAME_LIMIT < 0){
                        Main.MAX_FRAME_LIMIT = 0;
                    }
                    System.out.println(I18n.t("cli.setFrameDone", Main.MAX_FRAME_LIMIT));
                    saveConfig();
                    break;
                }
                case "2":{
                    Main.SHOW_REMAIN_HP = !Main.SHOW_REMAIN_HP;
                    System.out.println(Main.SHOW_REMAIN_HP ? I18n.t("cli.hpOn") : I18n.t("cli.hpOff"));
                    saveConfig();
                    break;
                }
                case "3":{
                    System.out.println(I18n.t("cli.reloading"));
                    Main.p1Raws = Setting.ReadFortsRaw("1P.txt");
                    Main.p2Raws = Setting.ReadFortsRaw("2P.txt");
                    Main.p1List = Setting.compileAll(Main.p1Raws);
                    Main.p2List = Setting.compileAll(Main.p2Raws);
                    System.out.println(I18n.t("cli.reloadDone"));
                    break;
                }
                case "4":   // 新增处理
                    System.out.print(I18n.t("cli.setThreadsPrompt"));
                    int newThreads = Integer.parseInt(scanner.nextLine());
                    if (newThreads < 1) newThreads = Runtime.getRuntime().availableProcessors();
                    Main.MAX_THREADS = newThreads;
                    if (Main.pool != null && !Main.pool.isShutdown()) {
                        Main.pool.shutdown();
                    }
                    Main.pool = Executors.newFixedThreadPool(Main.MAX_THREADS);
                    System.out.println(I18n.t("cli.setThreadsDone", Main.MAX_THREADS));
                    saveConfig();
                    break;
                case "9":{
                    return false;
                }
                default:{
                    System.out.println(I18n.t("cli.unknown"));
                    break;
                }
            }
        }
    }

    public static void loadConfig() {
        Properties prop = new Properties();
        try (FileInputStream fis = new FileInputStream(Main.CONFIG_FILE)) {
            prop.load(fis);
        } catch (Exception e) {
            System.out.println(I18n.t("cli.noConfig"));
        }
        Main.MAX_FRAME_LIMIT = Integer.parseInt(prop.getProperty("MAX_FRAME_LIMIT", "65536"));
        Main.SHOW_REMAIN_HP = Boolean.parseBoolean(prop.getProperty("SHOW_REMAIN_HP", "false"));
        Main.WORD_WRAP = Boolean.parseBoolean(prop.getProperty("WORD_WRAP", "false"));
        Main.DARK_MODE = Boolean.parseBoolean(prop.getProperty("DARK_MODE", "true"));
        Main.ACCENT_COLOR = prop.getProperty("ACCENT_COLOR", "#2675BF");
        String threadsProp = prop.getProperty("MAX_THREADS");
        if (threadsProp != null) {
            Main.MAX_THREADS = Integer.parseInt(threadsProp);
        } else {
            Main.MAX_THREADS = Runtime.getRuntime().availableProcessors();
        }
        Main.LANGUAGE = prop.getProperty("LANGUAGE", I18n.detectSystemLang());
        Main.SKIP_UPDATE_VERSION = prop.getProperty("SKIP_UPDATE_VERSION", "");
        I18n.setLang(Main.LANGUAGE);
    }

    public static void saveConfig() {
        Properties prop = new Properties();
        prop.setProperty("MAX_FRAME_LIMIT", String.valueOf(Main.MAX_FRAME_LIMIT));
        prop.setProperty("SHOW_REMAIN_HP", String.valueOf(Main.SHOW_REMAIN_HP));
        prop.setProperty("WORD_WRAP", String.valueOf(Main.WORD_WRAP));
        prop.setProperty("DARK_MODE", String.valueOf(Main.DARK_MODE));
        prop.setProperty("ACCENT_COLOR", Main.ACCENT_COLOR);
        prop.setProperty("MAX_THREADS", String.valueOf(Main.MAX_THREADS));
        prop.setProperty("LANGUAGE", Main.LANGUAGE);
        prop.setProperty("SKIP_UPDATE_VERSION", Main.SKIP_UPDATE_VERSION);

        try (FileOutputStream fos = new FileOutputStream(Main.CONFIG_FILE)) {
            prop.store(fos, "Game Config");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<CompiledFort> CompileForts(String fileName) {
        try {
            return parseForts(readFileText(fileName));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(I18n.t("cli.readFail", fileName));
            return new ArrayList<>();
        }
    }

    /** 读取文件并做与 {@link #parseForts} 相同的清洗/校验，返回原始阵型（名称 + 清洗后代码）。 */
    public static List<Fort> ReadFortsRaw(String fileName) {
        try {
            return parseFortsRaw(readFileText(fileName));
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(I18n.t("cli.readFail", fileName));
            return new ArrayList<>();
        }
    }

    private static String readFileText(String fileName) throws IOException {
        return readUtf8(Files.readAllBytes(Paths.get(fileName)));
    }

    public static List<CompiledFort> compileAll(List<Fort> forts) {
        List<CompiledFort> list = new ArrayList<>(forts.size());
        for (Fort fort : forts) {
            list.add(Main.compileFort(fort));
        }
        return list;
    }

    /** 解析 name&code（多条用 / 分隔）文本；自动剥离 #HP 后缀，畸形条目会被跳过（打印提示），不会抛异常。 */
    public static List<CompiledFort> parseForts(String content) {
        return compileAll(parseFortsRaw(content));
    }

    /** 与 {@link #parseForts} 相同的解析规则，但保留名称与清洗后的代码，供 Rust 模拟器直接使用。 */
    public static List<Fort> parseFortsRaw(String content) {
        List<Fort> list = new ArrayList<>();
        if (content == null) {
            return list;
        }
        String[] parts = content.trim().split("/");
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            int idx = part.lastIndexOf("&");
            String name;
            String code;
            if (idx == -1) {
                name = "";
                code = part;
            } else {
                name = part.substring(0, idx);
                code = part.substring(idx + 1);
            }
            int hash = code.indexOf('#');
            if (hash >= 0) {
                code = code.substring(0, hash);
            }
            code = code.replaceAll("[^a-zA-Z0-9]", "");
            if (idx == -1) {
                if (code.length() < 6) {
                    continue;
                }
                list.add(new Fort("", code));
            } else {
                if (code.length() < 6 || code.length() % 6 != 0) {
                    System.out.println(I18n.t("cli.badCode", name));
                    continue;
                }
                list.add(new Fort(name, code));
            }
        }
        return list;
    }

    public static void writeResult(String path, String content) {
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8))) {
            bw.write(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String readUtf8(byte[] bytes) {
        if (bytes.length == 0) return "";
        int start = 0;
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF
                && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            start = 3;
        }
        return new String(bytes, start, bytes.length - start, StandardCharsets.UTF_8);
    }
}