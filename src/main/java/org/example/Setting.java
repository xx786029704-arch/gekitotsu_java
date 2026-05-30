package org.example;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
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
            System.out.println("-====操作菜单====-");
            System.out.println("请选择操作：");
            System.out.println("0.开始对战");
            System.out.println("1.设置对局帧数上限（目前为" + Main.MAX_FRAME_LIMIT + "）");
            System.out.println(Main.SHOW_REMAIN_HP ? "2.关闭输出血量积分" : "2.开启输出血量积分");
            System.out.println("3.重新导入阵容");
            System.out.println("4.设置最大线程数（目前为" + Main.MAX_THREADS + "）");
            System.out.println("9.退出程序");
            System.out.println("输入对应数字以选择...");
            switch (scanner.nextLine()){
                case "0":{
                    return true;
                }
                case "1":{
                    System.out.print("设置对局帧数上限: ");
                    Main.MAX_FRAME_LIMIT = Integer.parseInt(scanner.nextLine());
                    if (Main.MAX_FRAME_LIMIT < 0){
                        Main.MAX_FRAME_LIMIT = 0;
                    }
                    System.out.println("对局帧数上限已经设置为" + Main.MAX_FRAME_LIMIT);
                    saveConfig();
                    break;
                }
                case "2":{
                    Main.SHOW_REMAIN_HP = !Main.SHOW_REMAIN_HP;
                    System.out.println(Main.SHOW_REMAIN_HP ? "将在simple_result.txt中记录血量积分" : "已关闭血量积分记录");
                    saveConfig();
                    break;
                }
                case "3":{
                    System.out.println("正在重新导入阵容...");
                    Main.p1List = Setting.CompileForts("1P.txt");
                    Main.p2List = Setting.CompileForts("2P.txt");
                    System.out.println("阵容导入完成！");
                    break;
                }
                case "4":   // 新增处理
                    System.out.print("设置最大线程数（0表示最大可用线程）: ");
                    int newThreads = Integer.parseInt(scanner.nextLine());
                    if (newThreads < 1) newThreads = Runtime.getRuntime().availableProcessors();
                    Main.MAX_THREADS = newThreads;
                    if (Main.pool != null && !Main.pool.isShutdown()) {
                        Main.pool.shutdown();
                    }
                    Main.pool = Executors.newFixedThreadPool(Main.MAX_THREADS);
                    System.out.println("最大线程数已设置为 " + Main.MAX_THREADS);
                    saveConfig();
                    break;
                case "9":{
                    return false;
                }
                default:{
                    System.out.println("未知操作");
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
            System.out.println("配置文件不存在，使用默认配置");
        }
        Main.MAX_FRAME_LIMIT = Integer.parseInt(prop.getProperty("MAX_FRAME_LIMIT", "65536"));
        Main.SHOW_REMAIN_HP = Boolean.parseBoolean(prop.getProperty("SHOW_REMAIN_HP", "false"));
        String threadsProp = prop.getProperty("MAX_THREADS");
        if (threadsProp != null) {
            Main.MAX_THREADS = Integer.parseInt(threadsProp);
        } else {
            Main.MAX_THREADS = Runtime.getRuntime().availableProcessors();
        }
    }

    public static void saveConfig() {
        Properties prop = new Properties();
        prop.setProperty("MAX_FRAME_LIMIT", String.valueOf(Main.MAX_FRAME_LIMIT));
        prop.setProperty("SHOW_REMAIN_HP", String.valueOf(Main.SHOW_REMAIN_HP));
        prop.setProperty("MAX_THREADS", String.valueOf(Main.MAX_THREADS));

        try (FileOutputStream fos = new FileOutputStream(Main.CONFIG_FILE)) {
            prop.store(fos, "Game Config");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<CompiledFort> CompileForts(String fileName) {
        List<CompiledFort> list = new ArrayList<>();
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(fileName));
            String content = decodeBytes(bytes).trim();
            // 移除 UTF-8 BOM（Windows 记事本默认会添加）
            if (!content.isEmpty() && content.charAt(0) == '﻿') {
                content = content.substring(1);
            }

            String[] parts = content.split("/");

            for (String part : parts) {
                if (part.isEmpty()){
                    continue;
                }
                part = part.trim();
                if (part.isEmpty()) continue;

                int idx = part.lastIndexOf("&");

                if (idx == -1) {
                    part = part.replaceAll("[^a-zA-Z0-9]", "");
                    if (part.length() < 6) continue;
                    list.add(Main.compileFort(new Fort("", part)));
                } else {
                    String name = part.substring(0, idx);
                    String code = part.substring(idx + 1);
                    code = code.replaceAll("[^a-zA-Z0-9]", "");
                    if (code.length() % 6 != 0){
                        System.out.println("阵"+name+"代码长度错误，已跳过");
                        continue;
                    }
                    list.add(Main.compileFort(new Fort(name, code)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("读取 " + fileName + " 失败");
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

    static String decodeBytes(byte[] bytes) {
        if (bytes.length == 0) return "";

        int bomOffset = 0;
        if (bytes.length >= 3 && bytes[0] == (byte) 0xEF
                && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF) {
            bomOffset = 3;
        }

        CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder();
        utf8Decoder.onMalformedInput(CodingErrorAction.REPORT);
        try {
            return utf8Decoder.decode(ByteBuffer.wrap(bytes, bomOffset, bytes.length - bomOffset)).toString();
        } catch (CharacterCodingException e) {
            // UTF-8 失败，尝试 GBK
        }

        try {
            return new String(bytes, bomOffset, bytes.length - bomOffset, Charset.forName("GBK"));
        } catch (Exception e) {
            return new String(bytes, bomOffset, bytes.length - bomOffset, Charset.defaultCharset());
        }
    }
}