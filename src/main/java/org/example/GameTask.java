package org.example;

import org.example.elements.*;
import org.example.elements.hit.KekkaiField;
import org.example.elements.units.*;
import org.example.elements.wall.*;

import java.util.ArrayList;

public class GameTask {

    public int ID = 0;   //待分配的ID，只会一直增长
    private boolean end = false;
    public int mid = 960;
    public boolean norikomi_flg = false;    //怒土の神秘小变量，撞击时会变成true
    public int[] hp = {100, 100};    //要塞血量
    public int[] hp0_flg = {0, 0};   //要塞爆炸标记
    public boolean[] dokkan_flg = {false, false};    //受到撞击标记，有贽玉不会变成true
    public int[] saihai_cnt = {0, 0};        //采玉效果剩余时间
    public int[] saihai_rot = {0, 0};        //采玉效果角度
    public int[] dead_last = {0, 0};         //上一个死亡的单位种类
    public float[] core_x = new float[]{0, 0};
    public float[] core_y = new float[]{0, 0};
    private int time = 0;     //计时器
    public Utils seeder = new Utils();
    public Base[] bases;     //双方车板
    public Core[] cores = new Core[]{null, null};    //双方老家
    public IntShapeMap elements = new IntShapeMap(1024);   //所有需要参与循环的部件
    public CompositeShape[] wall = {new CompositeShape(0,0), new CompositeShape(0,0)};   //墙体
    public UnitHitSystem[] unit = {new UnitHitSystem(this, 0,0), new UnitHitSystem(this, 0,0)};   //单位
    public CompositeShape[] shield = {new CompositeShape(0,0), new CompositeShape(0,0)}; //屏障
    public AtkHitSystem[] atk = {new AtkHitSystem(this, 0,0), new AtkHitSystem(this, 0,0)};    //攻击
    public CompositeShape[] fort = {new CompositeShape(0,0), new CompositeShape(0,0)};   //要塞主体
    public CompositeShape[] team = {new CompositeShape(0,0), new CompositeShape(0,0)};   //队伍
    public CompositeShape[] heal = {new CompositeShape(0,0), new CompositeShape(0,0)};   //治疗
    public CompositeShape[] repair = {new CompositeShape(0,0), new CompositeShape(0,0)}; //修复
    public CompositeShape[] jump_u = {new CompositeShape(0,0), new CompositeShape(0,0)}; //近突
    public CompositeShape[] jump_f = {new CompositeShape(0,0), new CompositeShape(0,0)}; //远突
    public CompositeShape[] snipe = {new CompositeShape(0,0), new CompositeShape(0,0)}; //狙击
    public CompositeShape[] turn_ccw = {new CompositeShape(0,0), new CompositeShape(0,0)}; //顺时针
    public CompositeShape[] turn_cw = {new CompositeShape(0,0), new CompositeShape(0,0)}; //逆时针
    public ArrayList<Integer>[] kekkaiIds = new ArrayList[]{new ArrayList<Integer>(), new ArrayList<Integer>()};
    public KekkaiField[] kekkaiFields = new KekkaiField[]{null, null};

    public int addElement(Shape s){      //增加新元素，返回id值
        elements.put(ID, s);
        return ID++;
    }

    public Result run_single(CompiledFort f1, CompiledFort f2){
        long start = System.nanoTime();
        init();
        main_setup(f1, f2);
        while (time < Main.MAX_FRAME_LIMIT && !end) {
            time++;
            base_move();
            judge();
            update();
            if (elements.deadCount > elements.size >> 1) {
                elements.compact();
            }
        }
        int status = -1;
        int winnerHp = -1;
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
        }
        return new Result(status, winnerHp, time, (System.nanoTime() - start) / 1000000.F);
    }

    private void base_move(){    //车板移动
        dokkan_flg[0] = false;
        dokkan_flg[1] = false;
        norikomi_flg = false;
        saihai_cnt[0]--;
        saihai_cnt[1]--;
        for (int i = 0; i <= 1; i++) {
            if (hp0_flg[i] > 0){
                continue;
            }
            core_x[i] = cores[i].x;
            core_y[i] = cores[i].y;
            for (Shape s : unit[i].getShapes()) {
                if (s instanceof TargetBall) {
                    core_x[i] = s.x;
                    core_y[i] = s.y;
                    break;
                }
            }
        }
        if (hp0_flg[0] == 0 && hp0_flg[1] == 0 && bases[1].x - bases[1].xs - (bases[0].x + bases[0].xs) <= 380) {
            norikomi_flg = true;
            float wrk;
            for (int i = 0; i <= 1; i++) {
                boolean nie_flg = false;
                for (Shape s : unit[i].getShapes()) {
                    if (s instanceof NieBall nie) {
                        nie.alarm = 6;
                        nie_flg = true;
                        break;
                    }
                }
                if (nie_flg){
                    i++;
                    continue;
                }
                wrk = (float) (Math.floor(bases[(1 - i)].xs * 5) + 1);
                wrk = Math.round(wrk);
                if (wrk < 0) {
                    wrk = 0;
                }
                hp[i] -= (int) wrk;
                dokkan_flg[i] = true;
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
        for (int i = 0; i <= 1; i++) {
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
                    hp[0] = Math.max(0, hp[0]);
                    hp[1] = Math.max(0, hp[1]);
                    end = true;
                    break;
                }
            }
        }
    }

    private void update() {  //单位更新
        if (hp0_flg[0] == 0 && hp0_flg[1] == 0){
            mid = (int) ((bases[0].x + bases[0].xs + bases[1].x - bases[1].xs) * 0.5F);
        }
        for (int i = 0; i <= 1; i++){
            unit[i].resign();
            atk[i].resign();
        }
        int toSize = elements.size;
        for (int i = 0; i < toSize; i++) {
            if (elements.items[i] != null) {
                elements.items[i].step();
            }
        }
    }

    private void init(){
        for (int i = 0; i <= 1; i++){
            fort[i].addShape(wall[i]);
            fort[i].addShape(unit[i]);
        }
        for (int i = 0; i <= 1; i++){
            team[i].addShape(wall[i]);
            team[i].addShape(unit[i]);
            team[i].addShape(shield[i]);
            team[i].addShape(atk[i]);
        }

        bases = new Base[]{new Base(this, 240, 532, 0), new Base(this, 1680, 532, 1)};
    }

    private void main_setup(CompiledFort f1, CompiledFort f2) {
        CompiledFort[] arr = {f1, f2};
        for (int i = 0; i < 2; i++) {
            CompiledFort f = arr[i];
            seeder.universalSeed *= ((f.coreX + 190) % 168 + 48) * ((f.coreY + 400) % 168 + 48);
            int cx = (i == 1 ? -f.coreX : f.coreX);
            int cy = f.coreY;
            switch (f.coreType) {
                case 1 -> cores[i] = new BossCore(this, cx, cy, i);
                case 2 -> cores[i] = new BossCore2(this, cx, cy, i);
                default -> cores[i] = new Core(this, cx, cy, i);
            }
            core_x[i] = cores[i].x;
            core_y[i] = cores[i].y;
            for (int k = 0; k < f.unitCount; k++) {
                float ux = f.x[k];
                float uy = f.y[k];
                int rr = f.r[k];
                if (i == 1) {
                    ux = (380 - ux) + 1490;
                    rr = 180 - rr;
                } else {
                    ux += 50;
                }
                uy += 132;
                rr = (rr % 360 + 360) % 360;
                unit_make(this, ux, uy, rr, f.type[k], i);
            }
            kekkaiFields[i] = new KekkaiField(this, i);
        }
    }

    public void unit_make(GameTask GAME, float X, float Y, int R, int TYPE, int S){   //创建单位
        int wrk = 0;
        switch (TYPE){
            case 1: new BowBall(GAME, X, Y, R, S, TYPE);break;
            case 2: new GunBall(GAME, X, Y, R, S, TYPE);break;
            case 3: new SwordBall(GAME, X, Y, R, S, TYPE);break;
            case 4: new TateBall(GAME, X, Y, R, S, TYPE);break;
            case 5: new BombBall(GAME, X, Y, R, S, TYPE);break;
            case 6: new MagicBall(GAME, X, Y, R, S, TYPE);break;
            case 7: new DokyuBall(GAME, X, Y, R, S, TYPE);break;
            case 8: new YariBall(GAME, X, Y, R, S, TYPE);break;
            case 9: new CannonBall(GAME, X, Y, R, S, TYPE);break;
            case 10: new NagiBall(GAME, X, Y, R, S, TYPE);break;
            case 11: new HaneBall(GAME, X, Y, R, S, TYPE);break;
            case 12: new RetsuBall(GAME, X, Y, R, S, TYPE);break;
            case 13: new ShotgunBall(GAME, X, Y, R, S, TYPE);break;
            case 14: new SniperBall(GAME, X, Y, R, S, TYPE);break;
            case 15: new UkiBall(GAME, X, Y, R, S, TYPE);break;
            case 16: new GuideBall(GAME, X, Y, R, S, TYPE);break;
            case 17: new RepairBall(GAME, X, Y, R, S, TYPE);break;
            case 18: new HealBall(GAME, X, Y, R, S, TYPE);break;
            case 19: new KabeBall(GAME, X, Y, R, S, TYPE);break;
            case 20: new TobiBall(GAME, X, Y, R, S, TYPE);break;
            case 21: new SenBall(GAME, X, Y, R, S, TYPE);break;
            case 22: new MinigunBall(GAME, X, Y, R, S, TYPE);break;
            case 23: new HenBall(GAME, X, Y, R, S, TYPE);break;
            case 24: new KekkaiBall(GAME, X, Y, R, S, TYPE);break;
            case 25: new Wood(GAME, X, Y, S, TYPE); wrk = 1; break;
            case 26: new Stone(GAME, X, Y, S, TYPE); wrk = 1; break;
            case 27: new Paper(GAME, X, Y, S, TYPE); wrk = 1; break;
            case 28: new Iron(GAME, X, Y, S, TYPE); wrk = 1; break;
            case 29: new Jet(GAME, X, Y, S, TYPE); wrk = 1; break;
            case 30: new Turbo(GAME, X, Y, S, TYPE); wrk = 1; break;
            case 31: new BoneBall(GAME, X, Y, R, S, TYPE);break;
            case 32: new GekiBall(GAME, X, Y, R, S, TYPE);break;
            case 33: new TonBall(GAME, X, Y, R, S, TYPE);break;
            case 34: new HolyBall(GAME, X, Y, R, S, TYPE);break;
            case 35: new NinBall(GAME, X, Y, R, S, TYPE);break;
            case 36: new SyouBall(GAME, X, Y, R, S, TYPE);break;
            case 37: new HanaBall(GAME, X, Y, R, S, TYPE);break;
            case 38: new HanBall(GAME, X, Y, R, S, TYPE);break;
            case 39: new PushBall(GAME, X, Y, R, S, TYPE);break;
            case 40: new GeiBall(GAME, X, Y, R, S, TYPE);break;
            case 41: new NieBall(GAME, X, Y, R, S, TYPE);break;
            case 42: new TargetBall(GAME, X, Y, R, S, TYPE);break;
            case 43: new TuiBall(GAME, X, Y, R, S, TYPE);break;
            case 44: new BoxBall(GAME, X, Y, R, S, TYPE);break;
            case 45: new DarkBall(GAME, X, Y, R, S, TYPE);break;
            case 46: new HeriBall(GAME, X, Y, R, S, TYPE);break;
            case 47: new SaiBall(GAME, X, Y, R, S, TYPE);break;
            case 48: new KnightBall(GAME, X, Y, R, S, TYPE);break;
            case 49: new KakuBall(GAME, X, Y, R, S, TYPE);break;
            case 50: new ShaBall(GAME, X, Y, R, S, TYPE);break;
            case 51: new StarBall(GAME, X, Y, R, S, TYPE);break;
            case 52: new ConBall(GAME, X, Y, R, S, TYPE);break;
            case 53: new KanBall(GAME, X, Y, R, S, TYPE);break;
            case 54: new SearchBall(GAME, X, Y, R, S, TYPE);break;
            case 55: new Near(GAME, X, Y, S, TYPE); wrk = 1; break;
            case 56: new Far(GAME, X, Y, S, TYPE); wrk = 1; break;
            case 57: new Wide(GAME, X, Y, S, TYPE); wrk = 1; break;
            case 58: new Narrow(GAME, X, Y, S, TYPE); wrk = 1; break;
            case 59: new Snipe(GAME, X, Y, S, TYPE); wrk = 1; break;
            case 60: new Elevator(GAME, X, Y, S, TYPE); wrk = 1; break;
            default: new Ball(GAME, X, Y, R, S, TYPE);break;
        }
        if (wrk == 0) {
            Ball unit = (Ball) elements.get(ID - 1);
            if (unit.side != 0) {
                unit.cnt = (int) (-(380 - (unit.x - 1490)) % unit.speed);
            } else {
                unit.cnt = (int) (-(unit.x - 50) % unit.speed);
            }
        }
    }
}
