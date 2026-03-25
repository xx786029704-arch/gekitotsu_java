package org.example;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

public class Setting {
    public Setting(){
        return;
    }

    public static boolean setting(Scanner scanner){
        while (true){
            System.out.println("请选择操作：");
            System.out.println("0.开始对战");
            System.out.println(Main.ENABLE_VISUALIZATION ? "1.关闭可视化" : "1.开启可视化");
            System.out.println(Main.SHOW_UNIT_HP ? "2.关闭单位血量显示" : "2.开启单位血量显示");
            System.out.println("3.设置帧率限制");
            System.out.println("9.退出程序");
            System.out.println("输入对应数字以选择...");
            switch (scanner.nextLine()){
                case "0":{
                    return true;
                }
                case "1":{
                    Main.ENABLE_VISUALIZATION = !Main.ENABLE_VISUALIZATION;
                    System.out.println(Main.ENABLE_VISUALIZATION ? "可视化已开启" : "可视化已关闭");
                    saveConfig();
                    break;
                }
                case "2":{
                    Main.SHOW_UNIT_HP = !Main.SHOW_UNIT_HP;
                    System.out.println(Main.SHOW_UNIT_HP ? "血量显示已开启" : "血量显示已关闭");
                    saveConfig();
                    break;
                }
                case "3":{
                    System.out.print("输入最大帧率（使用0代表无限）: ");
                    Main.LOGIC_TPS = Integer.parseInt(scanner.nextLine());
                    if (Main.LOGIC_TPS < 0){
                        Main.LOGIC_TPS = 0;
                    }
                    System.out.println("最大帧率已经设置为" + (Main.LOGIC_TPS == 0 ? "INF" : Main.LOGIC_TPS));
                    saveConfig();
                    break;
                }
                case "9":{
                    return false;
                }
                default:{
                    System.out.println("操作不正确");
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
        Main.ENABLE_VISUALIZATION = Boolean.parseBoolean(prop.getProperty("ENABLE_VISUALIZATION", "true"));
        Main.SHOW_UNIT_HP = Boolean.parseBoolean(prop.getProperty("SHOW_UNIT_HP", "true"));
        Main.LOGIC_TPS = Integer.parseInt(prop.getProperty("LOGIC_TPS", "90"));
    }

    public static void saveConfig() {
        Properties prop = new Properties();
        prop.setProperty("ENABLE_VISUALIZATION", String.valueOf(Main.ENABLE_VISUALIZATION));
        prop.setProperty("SHOW_UNIT_HP", String.valueOf(Main.SHOW_UNIT_HP));
        prop.setProperty("LOGIC_TPS", String.valueOf(Main.LOGIC_TPS));

        try (FileOutputStream fos = new FileOutputStream(Main.CONFIG_FILE)) {
            prop.store(fos, "Game Config");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<Fort> loadForts(String fileName) {
        List<Fort> list = new ArrayList<>();
        try {
            String content = new String(java.nio.file.Files.readAllBytes(
                    java.nio.file.Paths.get(fileName))).trim();

            String[] parts = content.split("/");

            for (String part : parts) {
                if (part.isEmpty()){
                    continue;
                }
                part = part.trim();
                if (part.isEmpty()) continue;

                int idx = part.lastIndexOf("&");

                if (idx == -1) {
                    list.add(new Fort("", part));
                } else {
                    String name = part.substring(0, idx);
                    String code = part.substring(idx + 1);
                    code = code.replaceAll("[^a-zA-Z0-9]", "");
                    if (code.length() % 6 != 0){
                        System.out.println("阵"+name+"代码长度错误，已跳过");
                        continue;
                    }
                    list.add(new Fort(name, code));
                }
            }
        } catch (Exception e) {
            System.out.println("读取 " + fileName + " 失败");
        }
        return list;
    }

    public static void writeResult(String path, String content) {
        try (FileWriter fw = new FileWriter(path)) {
            fw.write(content);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
