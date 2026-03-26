package org.example;

import org.example.elements.*;
import org.example.elements.hit.KekkaiField;
import org.example.elements.units.*;
import org.example.elements.wall.*;
import org.example.Settings;
import org.example.Result;

import java.util.*;

public class Game {
    private boolean end = false;
    public boolean norikomi_flg = false;    //怒土の神秘小变量，撞击时会变成true
    private int j;   //怒土遍历用的变量
    private int i;   //怒土遍历用的变量
    public static String pskey = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";      //密码表
    private static GameWindow window;
    public int ID = 0;   //待分配的ID，只会一直增长
    private float wrk;   //怒土の神秘小变量
    public int[] hp = {100, 100};    //要塞血量
    public int[] hp0_flg = {0, 0};   //要塞爆炸标记
    public boolean[] dokkan_flg = {false, false};    //受到撞击标记，有贽玉不会变成true
    public int[] saihai_cnt = {0, 0};        //采玉效果剩余时间
    public int[] saihai_rot = {0, 0};        //采玉效果角度
    public int[] dead_last = {0, 0};         //上一个死亡的单位种类
    public float[] core_x = new float[]{0, 0};
    public float[] core_y = new float[]{0, 0};
    public int time = 0;     //计时器
    public Utils[] seeder={new Utils(),new Utils()};
    public Base[] bases;     //双方车板
    public Core[] cores = new Core[]{null, null};    //双方老家
    //TODO：Gemini 说这里可以自己写可以以int为键的哈希表类，不用Integer包装类，至于提升大不大我不知道。
    public LinkedHashMap<Integer, Shape> elements = new LinkedHashMap<>();   //所有需要参与循环的部件
    public CompositeShape[] wall = {new CompositeShape(0,0), new CompositeShape(0,0)};   //墙体
    public CompositeShape[] unit = {new CompositeShape(0,0), new CompositeShape(0,0)};   //单位
    public CompositeShape[] shield = {new CompositeShape(0,0), new CompositeShape(0,0)}; //屏障
    public CompositeShape[] atk = {new CompositeShape(0,0), new CompositeShape(0,0)};    //攻击
    public CompositeShape[] fort = {new CompositeShape(0,0), new CompositeShape(0,0)};   //要塞主体
    public CompositeShape[] team = {new CompositeShape(0,0), new CompositeShape(0,0)};   //队伍
    public CompositeShape[] heal = {new CompositeShape(0,0), new CompositeShape(0,0)};   //治疗
    public CompositeShape[] repair = {new CompositeShape(0,0), new CompositeShape(0,0)}; //修复
    public CompositeShape[] jump_u = {new CompositeShape(0,0), new CompositeShape(0,0)}; //近突
    public CompositeShape[] jump_f = {new CompositeShape(0,0), new CompositeShape(0,0)}; //远突
    public CompositeShape[] snipe = {new CompositeShape(0,0), new CompositeShape(0,0)}; //狙击
    public CompositeShape[] turn_ccw = {new CompositeShape(0,0), new CompositeShape(0,0)}; //顺时针
    public CompositeShape[] turn_cw = {new CompositeShape(0,0), new CompositeShape(0,0)}; //逆时针
    public LinkedList<Integer>[] kekkaiIds = new LinkedList[]{new LinkedList<Integer>(), new LinkedList<Integer>()};
    public KekkaiField[] kekkaiFields = new KekkaiField[]{null, null};

    public Result run(String code1,String code2) {
        for (int i = 0; i <= 1; i++) {
            fort[i].addShape(wall[i]);
            fort[i].addShape(unit[i]);
        }
        for (int i = 0; i <= 1; i++) {
            team[i].addShape(wall[i]);
            team[i].addShape(unit[i]);
            team[i].addShape(shield[i]);
            team[i].addShape(atk[i]);
        }

        bases = new Base[]{new Base(this, 240, 532, 0), new Base(this, 1680, 532, 1)};

        if (!main_setup(new String[]{code1, code2})) {
            return new Result(-1, 0, 0, 0.f);
        }
        /*稍后处理
        if (ENABLE_VISUALIZATION) {
            java.awt.EventQueue.invokeLater(() -> {
                window = new GameWindow(1920, 960, 60).setList(new ArrayList<>(elements.values()));
                window.setVisible(true);
            });
        }

         */
        long timePerTick = Settings.LOGIC_TPS > 0 ? 1000000000L / Settings.LOGIC_TPS : 0;
        long lastTime = System.nanoTime();
        long startTime = lastTime;

        while (time < Settings.max_run_time && !end) {    /*主循环*/
            long now = System.nanoTime();
            if (timePerTick == 0 || now - lastTime >= timePerTick) {
                lastTime = now;
                time++;
                base_move();    //要塞车移动
                judge();    //判断对局是否应该结束
                update();   //调用所有元素step()方法
                /*稍后处理
                if (ENABLE_VISUALIZATION && window != null) {
                    window.setList(new ArrayList<>(elements.values()));
                    window.requestRender();
                }

                 */
            }
        }
        long endTime = System.nanoTime();
        int status = -1;
        int winnerHp = 0;
        System.out.println("1P血量: " + hp[0] + ", 2P血量: " + hp[1]);
        if (hp0_flg[0] > 0 || hp0_flg[1] > 0) {
            if (hp0_flg[0] == 0) {
                status = 1;
                winnerHp = hp[0];
            } else if (hp0_flg[1] == 0) {
                status = 2;
                winnerHp = hp[1];
            } else {
                status = 0;
            }
        } else {
            status = -1;
        }
        return new Result(status, winnerHp, time, (endTime - startTime) / 1000000000.F);
    }

    private boolean main_setup(String[] code) {      //初始布局
        i = 0;
        while (i <= 1) {
            code[i] = code[i].replaceAll("[^a-zA-Z0-9]", "");
            if (code[i].length() % 6 != 0) {
                return false;
            }
            int[] xyr = to_xyr(code[i].substring(1, 6));
            if (xyr[0] < 52 || xyr[0] > 328 || xyr[1] < 58 || xyr[1] > 334) return false;
            Utils.universalSeed = Utils.universalSeed * (xyr[0] % 168 + 48) * (xyr[1] % 168 + 48);
            int baseSeed = (xyr[0] % 168 + 48) * (xyr[1] % 168 + 48);
            seeder[i].setSeed(baseSeed);
            xyr[0] -= 190;
            xyr[1] -= 400;
            if (code[i].startsWith("1")) {
                cores[i] = new BossCore(this, i == 1 ? -xyr[0] : xyr[0], xyr[1], i);
            } else if (code[i].startsWith("2")) {
                cores[i] = new BossCore2(this, i == 1 ? -xyr[0] : xyr[0], xyr[1], i);
            } else {
                cores[i] = new Core(this, i == 1 ? -xyr[0] : xyr[0], xyr[1], i);
            }
            core_x[i] = cores[i].x;
            core_y[i] = cores[i].y;
            int wrkSeed;
            j = 6;
            int wrkType;
            while (j < code[i].length()) {
                xyr = to_xyr(code[i].substring(j + 1, j + 6));
                if (xyr[0] < 16 || xyr[1] > 364 || xyr[1] < 20 || xyr[1] > 369 || xyr[2] > 360) return false;
                wrkSeed = baseSeed * (xyr[0] % 185 + 30) * (xyr[1] % 185 + 30);
                if (i == 1) {
                    xyr[0] = (380 - xyr[0]) + 1490;
                    xyr[2] = 180 - xyr[2];
                } else {
                    xyr[0] += 50;
                }
                xyr[2] = (xyr[2] % 360 + 360) % 360;
                xyr[1] += 132;
                wrkType = pskey.indexOf(code[i].charAt(j));
                if (wrkType < 1 || wrkType > 60) return false;
                unit_make(xyr[0], xyr[1], xyr[2], wrkType, i);
                if (wrkType == 13) {
                    ShotgunBall unit = (ShotgunBall) elements.get(ID - 1);
                    unit.setSeed(wrkSeed);
                } else if (wrkType == 23) {
                    HenBall unit = (HenBall) elements.get(ID - 1);
                    unit.setSeed(wrkSeed);
                }
                j += 6;
            }
            kekkaiFields[i] = new KekkaiField(this, i);   //界玉初始化
            i++;
        }
        return true;
    }

    private void base_move(){    //车板移动
        dokkan_flg[0] = false;
        dokkan_flg[1] = false;
        norikomi_flg = false;
        saihai_cnt[0]--;
        saihai_cnt[1]--;
        j = 0;
        while (j <= 1) {
            if (hp0_flg[j] > 0){
                j++;
                continue;
            }
            core_x[j] = cores[j].x;
            core_y[j] = cores[j].y;
            for (Shape s : unit[j].getShapes()) {
                if (s instanceof TargetBall) {
                    core_x[j] = s.x;
                    core_y[j] = s.y;
                    break;
                }
            }
            j++;
        }
        if (hp0_flg[0] == 0 && hp0_flg[1] == 0 && bases[1].x - bases[1].xs - (bases[0].x + bases[0].xs) <= 380) {
            norikomi_flg = true;
            System.out.println("要塞相撞，时间: " + time);
            j = 0;
            while (j <= 1) {
                boolean nie_flg = false;
                for (Shape s : unit[j].getShapes()) {
                    if (s instanceof NieBall nie) {
                        nie.alarm = 6;
                        nie_flg = true;
                        break;
                    }
                }
                if (nie_flg){
                    j++;
                    continue;
                }
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

    private void judge(){    //裁决爆炸
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
                    hp[0] = Math.max(0, hp[0]);
                    hp[1] = Math.max(0, hp[1]);
                    end = true;
                    break;
                }
            }
            i++;
        }
    }

    private void update() {  //单位更新
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

    public int addElement(Shape s){      //增加新元素，返回id值
        elements.put(ID, s);
        return ID++;
    }

    /**
     * @param X x
     * @param Y y
     * @param R 角度
     * @param TYPE 种类 (1-60 间整数)
     * @param S 所属方 (0 或 1)
     */
    public void unit_make(float X, float Y, int R, int TYPE, int S){   //创建单位
        int wrk = 0;
        switch (TYPE){
            case 1: new BowBall(this, X, Y, R, S, TYPE);break;
            case 2: new GunBall(this, X, Y, R, S, TYPE);break;
            case 3: new SwordBall(this, X, Y, R, S, TYPE);break;
            case 4: new TateBall(this, X, Y, R, S, TYPE);break;
            case 5: new BombBall(this, X, Y, R, S, TYPE);break;
            case 6: new MagicBall(this, X, Y, R, S, TYPE);break;
            case 7: new DokyuBall(this, X, Y, R, S, TYPE);break;
            case 8: new YariBall(this, X, Y, R, S, TYPE);break;
            case 9: new CannonBall(this, X, Y, R, S, TYPE);break;
            case 10: new NagiBall(this, X, Y, R, S, TYPE);break;
            case 11: new HaneBall(this, X, Y, R, S, TYPE);break;
            case 12: new RetsuBall(this, X, Y, R, S, TYPE);break;
            case 13: new ShotgunBall(this, X, Y, R, S, TYPE);break;
            case 14: new SniperBall(this, X, Y, R, S, TYPE);break;
            case 15: new UkiBall(this, X, Y, R, S, TYPE);break;
            case 16: new GuideBall(this, X, Y, R, S, TYPE);break;
            case 17: new RepairBall(this, X, Y, R, S, TYPE);break;
            case 18: new HealBall(this, X, Y, R, S, TYPE);break;
            case 19: new KabeBall(this, X, Y, R, S, TYPE);break;
            case 20: new TobiBall(this, X, Y, R, S, TYPE);break;
            case 21: new SenBall(this, X, Y, R, S, TYPE);break;
            case 22: new MinigunBall(this, X, Y, R, S, TYPE);break;
            case 23: new HenBall(this, X, Y, R, S, TYPE);break;
            case 24: new KekkaiBall(this, X, Y, R, S, TYPE);break;
            case 25: new Wood(this, X, Y, S, TYPE); wrk = 1; break;
            case 26: new Stone(this, X, Y, S, TYPE); wrk = 1; break;
            case 27: new Paper(this, X, Y, S, TYPE); wrk = 1; break;
            case 28: new Iron(this, X, Y, S, TYPE); wrk = 1; break;
            case 29: new Jet(this, X, Y, S, TYPE); wrk = 1; break;
            case 30: new Turbo(this, X, Y, S, TYPE); wrk = 1; break;
            case 31: new BoneBall(this, X, Y, R, S, TYPE);break;
            case 32: new GekiBall(this, X, Y, R, S, TYPE);break;
            case 33: new TonBall(this, X, Y, R, S, TYPE);break;
            case 34: new HolyBall(this, X, Y, R, S, TYPE);break;
            case 35: new NinBall(this, X, Y, R, S, TYPE);break;
            case 36: new SyouBall(this, X, Y, R, S, TYPE);break;
            case 37: new HanaBall(this, X, Y, R, S, TYPE);break;
            case 38: new HanBall(this, X, Y, R, S, TYPE);break;
            case 39: new PushBall(this, X, Y, R, S, TYPE);break;
            case 40: new GeiBall(this, X, Y, R, S, TYPE);break;
            case 41: new NieBall(this, X, Y, R, S, TYPE);break;
            case 42: new TargetBall(this, X, Y, R, S, TYPE);break;
            case 43: new TuiBall(this, X, Y, R, S, TYPE);break;
            case 44: new BoxBall(this, X, Y, R, S, TYPE);break;
            case 45: new DarkBall(this, X, Y, R, S, TYPE);break;
            case 46: new HeriBall(this, X, Y, R, S, TYPE);break;
            case 47: new SaiBall(this, X, Y, R, S, TYPE);break;
            case 48: new KnightBall(this, X, Y, R, S, TYPE);break;
            case 49: new KakuBall(this, X, Y, R, S, TYPE);break;
            case 50: new ShaBall(this, X, Y, R, S, TYPE);break;
            case 51: new StarBall(this, X, Y, R, S, TYPE);break;
            case 52: new ConBall(this, X, Y, R, S, TYPE);break;
            case 53: new KanBall(this, X, Y, R, S, TYPE);break;
            case 54: new SearchBall(this, X, Y, R, S, TYPE);break;
            case 55: new Near(this, X, Y, S, TYPE); wrk = 1; break;
            case 56: new Far(this, X, Y, S, TYPE); wrk = 1; break;
            case 57: new Wide(this, X, Y, S, TYPE); wrk = 1; break;
            case 58: new Narrow(this, X, Y, S, TYPE); wrk = 1; break;
            case 59: new Snipe(this, X, Y, S, TYPE); wrk = 1; break;
            case 60: new Elevator(this, X, Y, S, TYPE); wrk = 1; break;
            default: new Ball(this, X, Y, R, S, TYPE);break;
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
