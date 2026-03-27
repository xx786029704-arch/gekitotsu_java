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
            System.out.println("-====操作菜单====-");
            System.out.println("请选择操作：");
            System.out.println("0.开始对战");
            System.out.println("1.设置对局帧数上限（目前为" + Main.MAX_FRAME_LIMIT + "）");
            System.out.println(Main.SHOW_REMAIN_HP ? "2.关闭输出血量积分" : "2.开启输出血量积分");
            System.out.println("3.重新导入阵容");
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
        Main.AUTO_PLAY = Boolean.parseBoolean(prop.getProperty("AUTO_PLAY", "false"));
    }

    public static void saveConfig() {
        Properties prop = new Properties();
        prop.setProperty("MAX_FRAME_LIMIT", String.valueOf(Main.MAX_FRAME_LIMIT));
        prop.setProperty("SHOW_REMAIN_HP", String.valueOf(Main.SHOW_REMAIN_HP));
        prop.setProperty("AUTO_PLAY", String.valueOf(Main.AUTO_PLAY));

        try (FileOutputStream fos = new FileOutputStream(Main.CONFIG_FILE)) {
            prop.store(fos, "Game Config");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<CompiledFort> CompileForts(String fileName) {
        List<CompiledFort> list = new ArrayList<>();
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
