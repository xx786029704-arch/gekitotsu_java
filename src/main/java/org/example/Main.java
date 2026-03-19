package org.example;

import org.example.elements.Ball;
import org.example.elements.Base;
import org.example.elements.Core;
import org.example.elements.units.BowBall;
import org.example.elements.units.BombBall;
import org.example.elements.units.CannonBall;
import org.example.elements.units.GuideBall;
import org.example.elements.units.GekiBall;
import org.example.elements.units.GunBall;
import org.example.elements.units.SwordBall;
import org.example.elements.units.KakuBall;
import org.example.elements.units.DokyuBall;
import org.example.elements.units.TonBall;
import org.example.elements.units.NinBall;
import org.example.elements.units.HaneBall;
import org.example.elements.units.ShotgunBall;
import org.example.elements.units.RetsuBall;
import org.example.elements.units.HanaBall;

import java.util.*;

public class Main {
    public static final boolean ENABLE_VISUALIZATION = true;    //是否开启可视化
    public static final int LOGIC_TPS = 10;      //帧率限制，0代表无限制

    static boolean end = false;
    static boolean norikomi_flg = false;    //怒土の神秘小变量，撞击时会变成true
    static int j;   //怒土遍历用的变量
    static int i;   //怒土遍历用的变量
    static int pointer;
    public static String pskey = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";      //密码表
    public static int ID = 0;   //待分配的ID，只会一直增长
    static float wrk;   //怒土の神秘小变量
    static int max_run_time = 65536;    //最大运行帧数
    static Scanner scanner = new Scanner(System.in);
    public static int[] hp = {100, 100};    //要塞血量
    public static int[] hp0_flg = {0, 0};   //要塞爆炸标记
    public static boolean[] dokkan_flg = {false, false};    //受到撞击标记，有贽玉不会变成true
    public static Base[] bases;     //双方车板
    public static Core[] cores = new Core[]{null, null};    //双方老家
    public static int time = 0;     //计时器
    public static LinkedHashMap<Integer, Shape> elements = new LinkedHashMap<>();   //所有需要参与循环的部件
    public static CompositeShape[] wall = {new CompositeShape(0,0), new CompositeShape(0,0)};   //墙体
    public static CompositeShape[] unit = {new CompositeShape(0,0), new CompositeShape(0,0)};   //单位
    public static CompositeShape[] shield = {new CompositeShape(0,0), new CompositeShape(0,0)}; //屏障
    public static CompositeShape[] atk = {new CompositeShape(0,0), new CompositeShape(0,0)};    //攻击
    public static CompositeShape[] team = {new CompositeShape(0,0), new CompositeShape(0,0)};   //队伍
    public static CompositeShape[] heal = {new CompositeShape(0,0), new CompositeShape(0,0)};   //治疗
    public static CompositeShape[] repair = {new CompositeShape(0,0), new CompositeShape(0,0)}; //修复
    static String default_code = "000Nuzxk5t8nxk5vOmxk5AEoxk5Fuvx6dvxGx6dOCw vs 000P6R";    //默认对战代码，为空时在运行时手动输入

    public static void main(String[] args) {
        for (int i = 0; i <= 1; i++){
            team[i].addShape(wall[i]);
            team[i].addShape(unit[i]);
            team[i].addShape(shield[i]);
            team[i].addShape(atk[i]);
        }

        bases = new Base[]{new Base(240, 532, 0), new Base(1680, 532, 1)};

        if (default_code.isEmpty()) {   //处理代码导入
            System.out.println("请输入对战代码：(输入格式: 1P代码 vs 2P代码)");
            String[] input = scanner.nextLine().split(" vs ");
            main_setup(input);
        } else {
            main_setup(default_code.split(" vs "));
        }
        if (ENABLE_VISUALIZATION) {
            java.awt.EventQueue.invokeLater(() -> {
                GameWindow window = new GameWindow(1920, 960, 60);
                window.setVisible(true);
            });
        }
        long timePerTick = LOGIC_TPS > 0 ? 1000000000L / LOGIC_TPS : 0;
        long lastTime = System.nanoTime();

        while (time < max_run_time && !end){    /*主循环*/
            long now = System.nanoTime();
            if (timePerTick == 0 || now - lastTime >= timePerTick) {
                lastTime = now;
                time++;
                base_move();    //要塞车移动
                judge();    //判断对局是否应该结束
                update();   //调用所有元素step()方法
            }
        }
    }

    private static void game_set(){     //判断局势
        System.out.println("对局结束，用时: " + time);
        System.out.println("1P血量: " + hp[0] + ", 2P血量: " + hp[1]);
        System.out.println(hp0_flg[0]);
        System.out.println(hp0_flg[1]);
        if (hp0_flg[0] > 0 || hp0_flg[1] > 0){
            if(hp0_flg[0] == 0){
                System.out.println("1P获胜");
            }else if(hp0_flg[1] == 0) {
                System.out.println("2P获胜");
            }else {
                System.out.println("平局");
            }
        }else {
            System.out.println("未定");
        }
    }

    private static void main_setup(String[] code){      //初始布局
        i = 0;
        while (i <= 1) {
            code[i] = code[i].replaceAll("[^a-zA-Z0-9]", "");
            if (code[i].length() % 6 != 0){
                return;
            }
            int[] xyr = to_xyr(code[i].substring(1,6));
            xyr[0] -= 190;
            xyr[1] -= 400;
            if (code[i].startsWith("0")){
                cores[i] = new Core(i == 1 ? -xyr[0] : xyr[0], xyr[1], i);
            }
            i++;
        }
        i = 0;
        while (i <= 1) {
            j = 6;
            while (j < code[i].length()) {
                int[] xyr = to_xyr(code[i].substring(j+1, j+6));
                if (i == 1) {
                    xyr[0] = (380 - xyr[0]) + 1490;
                    xyr[2] = 180 - xyr[2];
                } else {
                    xyr[0] += 50;
                }
                xyr[2] = (xyr[2] % 360 + 360) % 360;
                xyr[1] += 132;
                unit_make(xyr[0], xyr[1], xyr[2], pskey.indexOf(code[i].charAt(j)), i);
                j += 6;
            }
            i++;
        }
    }

    private static void base_move(){    //车板移动
        dokkan_flg[0] = false;
        dokkan_flg[1] = false;
        norikomi_flg = false;
        if (hp0_flg[0] == 0 && hp0_flg[1] == 0 && bases[1].x - bases[1].xs - (bases[0].x + bases[0].xs) <= 380) {
            norikomi_flg = true;
            System.out.println("要塞相撞，时间: " + time);
            j = 0;
            while (j <= 1) {
                wrk = (float) (Math.floor(bases[(1 - j)].xs * 5) + 1);
                wrk = Math.round(wrk);
                if (wrk < 0) {
                    wrk = 0;
                }
                hp[j] -= (int) wrk;
                dokkan_flg[j] = true;
                j++;
            }
            wrk = bases[0].xs;
            bases[0].xs = -bases[1].xs;
            if (bases[0].xs > -1) { bases[0].xs = -1; }
            bases[1].xs = -wrk;
            if (bases[1].xs > -1) { bases[1].xs = -1; }
            bases[0].ys = (-bases[0].xs) * 2;
            bases[1].ys = (-bases[1].xs) * 2;
        }
    }

    private static void judge(){    //裁决爆炸
        i = 0;
        while (i <= 1) {
            if (hp0_flg[i] == 0 && hp[i] < 1) {
                hp0_flg[i] = 1;
                dokkan_flg[i] = true;
            }
            if (hp0_flg[i] > 0) {
                hp0_flg[i]++;
                dokkan_flg[i] = true;
                if (hp0_flg[i] == 3) {
                    bases[i].kill();
                    cores[i].kill();
                }
                if (hp0_flg[i] > 120) {
                    wrk = 0;
                    game_set();
                    end = true;
                    break;
                }
            }
            i++;
        }
    }

    private static void update() {  //单位更新
        List<Integer> keys = new ArrayList<>(elements.keySet());

        for (Integer key : keys) {
            if (!elements.containsKey(key)) {
                continue;
            }
            elements.get(key).step();
        }
    }

    private static int[] to_xyr(String str){    //5位61进制转x y r数组
        int rxy = 0;
        rxy += pskey.indexOf(str.charAt(0)) * 13845841;
        rxy += pskey.indexOf(str.charAt(1)) * 226981;
        rxy += pskey.indexOf(str.charAt(2)) * 3721;
        rxy += pskey.indexOf(str.charAt(3)) * 61;
        rxy += pskey.indexOf(str.charAt(4));
        String rxy_str = String.format("%09d", rxy);
        return new int[]{Integer.parseInt(rxy_str.substring(3, 6)), Integer.parseInt(rxy_str.substring(6, 9)), Integer.parseInt(rxy_str.substring(0, 3))};
    }

    public static int addElement(Shape s){      //增加新元素，返回id值
        elements.put(ID, s);
        return ID++;
    }

    public static void unit_make(float X, float Y, float R, int TYPE, int S){   //创建单位
        int wrk = 0;
        switch (TYPE){
            case 1: new BowBall(X, Y, R, S, TYPE);break;
            case 2: new GunBall(X, Y, R, S, TYPE);break;
            case 3: new SwordBall(X, Y, R, S, TYPE);break;
            case 5: new BombBall(X, Y, R, S, TYPE);break;
            case 7: new DokyuBall(X, Y, R, S, TYPE);break;
            case 9: new CannonBall(X, Y, R, S, TYPE);break;
            case 11: new HaneBall(X, Y, R, S, TYPE);break;
            case 12: new RetsuBall(X, Y, R, S, TYPE);break;
            case 13: new ShotgunBall(X, Y, R, S, TYPE);break;
            case 16: new GuideBall(X, Y, R, S, TYPE);break;
            case 32: new GekiBall(X, Y, R, S, TYPE);break;
            case 33: new TonBall(X, Y, R, S, TYPE);break;
            case 35: new NinBall(X, Y, R, S, TYPE);break;
            case 37: new HanaBall(X, Y, R, S, TYPE);break;
            case 49: new KakuBall(X, Y, R, S, TYPE);break;
            default: new Ball(X, Y, R, S, TYPE);break;
        }
        switch (wrk){
            case 0: {
                Ball unit = (Ball) elements.get(ID - 1);
                if (unit.side != 0) {
                    unit.cnt = (int) (-(380 - (unit.x - 1490)) % unit.speed);
                }
                else {
                    unit.cnt = (int) (-(unit.x - 50) % unit.speed);
                }
                break;
            }
            case 1: break;
            default: break;
        }
    }
}
