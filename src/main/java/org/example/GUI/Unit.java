package org.example.GUI;

import org.example.CompiledFort;
import org.example.Main;
import org.example.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Unit {
    public int x;
    public int y;
    public int hp;
    public int id;
    public int r;

    public static Info[] infos = {
            new Info(0, 0, "要塞核心", 100, -1, 0,0, 0),
            new Info(30, 0, "弓玉",10,30, 6,6, 2),
            new Info(70, 0, "铳玉",10,60, 4, 0, 6),
            new Info(30, 0, "剑玉",30,10, 0, 0, 4),
            new Info(60, 0, "盾玉",30,-1, -1, 0, 0),
            new Info(50, 0, "爆玉",10,80, 1, 1, 5),
            new Info(200, 0, "魔玉",10,150, 38, 1, 36),
            new Info(30, 0, "弩玉",10,30, 6, 6, 2),
            new Info(30, 0, "枪玉",30,15, 0, 0, 5),
            new Info(70, 2, "炮玉",10,120, 0, 0, 5),
            new Info(30, 3, "剃玉",30,20, 0, 0, 10),
            new Info(70, 6, "跳玉",10,50, 0, 0, 2),
            new Info(70, 1, "裂玉",10,120, 1, 1, 14002),
            new Info(50, 1, "散玉",10,50, 0, 0, 18002),
            new Info(70, 11, "狙玉",10,100, 0, 0, 3),
            new Info(70, 2, "浮玉",10,150, 8, 8, 4),
            new Info(80, 12, "导玉",10,180, 0, 0, 5),
            new Info(100, 5, "缮玉",10,150, 4, 4, 2001),
            new Info(100, 5, "愈玉",10,150, 4, 4, 2001),
            new Info(150, 12, "壁玉",20,60, 301, 0, 0),
            new Info(150, 10, "飞玉",10,300, 4, 4, 5),
            new Info(150, 14, "战玉",10,300, 3, 0, 5),
            new Info(200, 15, "机玉",10,200, 200, 0, 200),
            new Info(100, 13, "变玉",10,100, 8, 8, 7.57142857F),
            new Info(90, 15, "界玉",20,-1, -1, 0, 0),
            new Info(15, 0, "木要塞壁",35,-1, -1, 0, 0),
            new Info(40, 0, "石要塞壁",75,-1, -1, 0, 0),
            new Info(5, 0, "纸要塞壁",1,-1, -1, 0, 0),
            new Info(100, 8, "铁要塞壁",150,-1, -1, 0, 0),
            new Info(100, 4, "红加速器",15,-1, -1, 0, 0),
            new Info(250, 4, "蓝加速器",100,-1, -1, 0, 0),
            new Info(10, 0, "骨玉",10,30, 0, 0, 4),
            new Info(70, 2, "击玉",10,120, 0, 0, 5),
            new Info(60, 6, "弹玉",10,70, 3, 3, 2),
            new Info(200, 8, "圣玉",10,300, 6, -1, 0),
            new Info(70, 6, "忍玉",10,70, 3, 3, 2),
            new Info(60, 10, "障玉",10,160, 7, 7, 1),
            new Info(70, 1, "花玉",10,120, 0, 0, 40002),
            new Info(60, 3, "反玉",15,20, 6, -1, 0),
            new Info(60, 4, "押玉",10,80, 6, 6, 2),
            new Info(60, 3, "迎玉",15,40, 6, -1, 0),
            new Info(150, 8, "贽玉",30,-1, -1, 0, 0),
            new Info(50, 13, "的玉",30,-1, -1, 0, 0),
            new Info(150, 14, "坠玉",10,300, 3, 3, 5),
            new Info(70, 7, "箱玉",15,200, 4, -1, 0),
            new Info(150, 15, "暗玉",10,200, 4, 4, 0),
            new Info(150, 10, "旋玉",10,300, 4, 4, 5),
            new Info(20, 11, "采玉",15,200, 5, 5, 0),
            new Info(150, 9, "骑玉",50,7, 0, 0, 4),
            new Info(200, 14, "核玉",10,350, 4, 4, 5),
            new Info(100, 14, "射玉",10,70, 0, 0, 6),
            new Info(200, 15, "星玉",10,250, 31, 1, 44),
            new Info(200, 12, "捆玉",10,200, 0, 0, 16005),
            new Info(150, 11, "贯玉",10,170, 7, 7, 2),
            new Info(200, 13, "查玉",10,100, 4, 4, 3),
            new Info(40, 9, "近突击壁",35,-1, 0, 0, 0),
            new Info(50, 9, "远突击壁",35,900, 0, 0, 0),
            new Info(80, 7, "紫旋转壁",35,40, 0, 0, 0),
            new Info(80, 7, "蓝旋转壁",35,16, 0, 0, 0),
            new Info(200, 13, "狙击壁",50,-1, -1, 0, 0),
            new Info(60, 5, "电梯壁",75,-1, -1, 0, 0),
            new Info(750, 16, "BOSS要塞",100,-1, -1, 0, 0),
            new Info(9999, -1, "生气的BOSS要塞",100,301, 59, 2, 0),
            new Info(-1, -1, "未知单位",0,-1, -1, 0, 0)
    };

    public Unit(int id, int x, int y, int r) {
        this.x = x;
        this.y = y;
        this.id = Math.clamp(id, 0, 63);
        this.r = r;
        this.hp = infos[id].hp;
    }

    public String encode(){
        if (id < 0) return "";
        return Main.pskey.charAt(id % 61 + id / 61) + Utils.to_rad61(r * 1000000 + x * 1000 + y + (isCore() ? 52058 : 16020), 5);
    }

    public static Unit decode(String str){
        int[] xyr = Main.to_xyr(str.substring(1));
        Unit unit = new Unit(Main.pskey.indexOf(str.charAt(0)), xyr[0] - 16, xyr[1] - 20, xyr[2]);
        if (unit.id > 63 || unit.id < 0) {
            unit.id = 63;
        }
        return unit;
    }

    public static Unit decodeAsCore(String str){
        int[] rxy = Main.to_xyr(str.substring(1));
        Unit unit = new Unit(Main.pskey.indexOf(str.charAt(0)), rxy[0] - 52, rxy[1] - 58, rxy[2]);
        if (unit.id > 1) {
            unit.id = 62;
        }
        else if (unit.id < 1) {
            unit.id = 0;
        }
        else {
            unit.id = 61;
        }
        return unit;
    }

    public boolean isWall() {
        return (24 < id && id <= 30) || (54 < id && id <= 60);
    }

    public boolean isWallLike() {
        return isWall() || isCore();
    }

    public boolean isCore() {
        return (id == 0 || id == 61 || id == 62);
    }

    public static boolean isWall(int ID) {
        return (24 < ID && ID <= 30) || (54 < ID && ID <= 60);
    }

    public static boolean isWallLike(int ID) {
        return isWall(ID) || isCore(ID);
    }

    public static boolean isCore(int ID) {
        return (ID == 0 || ID == 61 || ID == 62);
    }

    public static float getDps(int ID, float dmg) {
        return dmg * 100 / (infos[ID].cd + infos[ID].at);
    }

    public static float getDpsPer100Cost(int ID, float dmg) {
        return dmg * 10000 / (infos[ID].cd + infos[ID].at) / infos[ID].cost;
    }

    public int getDelay(){
        if (infos[id].cd < 0) return -1;
        if (id == 56 || id == 57) return -(x + 16) % infos[id].cd;
        if (id == 58) return -8;
        if (id == 62) return 0;
        return (x + 16) % infos[id].cd;
    }

    public String getLabel(){
        return "(" + x + ", " + y + (isWallLike() ? ")" : (", " + r + ")"));
    }

    //获取该种单位在突击要塞壁上的最速行动坐标集
    public List<Integer> getQuickestXList(int wallX, boolean isFirst){
        int cd = infos[id].cd;
        int st = infos[id].shoot;
        List<Integer> MayX = new ArrayList<>();
        if (cd < 0  || st < 0 ||isWallLike()) return MayX;
        int atk = 884 - wallX + (isFirst ? 1 : 0);
        int atkInCycle = cd + st - 1;
        int t_delay = (atk - atkInCycle) % (cd + infos[id].at);
        t_delay = t_delay > cd ? 0 : t_delay;
        int X = wallX + 17;
        int dx = t_delay - (X + 16) % cd;
        int next = dx >= 0 ? X + dx : X + dx + cd;
        X = wallX - 16;
        dx = t_delay - wallX % cd;
        int last = dx <= 0 ? X + dx : X + dx - cd;
        while (last < next){
            if (last >= wallX - 16 && last <= wallX + 17 && last >= 0 && last <= 348) {
                MayX.add(last);
            }
            last += cd;
        }
        return MayX;
    }

    public static List<Integer> getQuickestXList(int ID, int wallX, boolean isFirst){
        int cd = infos[ID].cd;
        int st = infos[ID].shoot;
        List<Integer> MayX = new ArrayList<>();
        if (cd < 0  || st < 0 ||isWallLike(ID)) return MayX;
        int atk = 884 - wallX + (isFirst ? 1 : 0);
        int atkInCycle = cd + st - 1;
        int t_delay = (atk - atkInCycle) % (cd + infos[ID].at);
        t_delay = t_delay > cd ? 0 : t_delay;
        int X = wallX + 17;
        int dx = t_delay - (X + 16) % cd;
        int next = dx >= 0 ? X + dx : X + dx + cd;
        X = wallX - 16;
        dx = t_delay - wallX % cd;
        int last = dx <= 0 ? X + dx : X + dx - cd;
        while (last < next){
            if (last >= wallX - 16 && last <= wallX + 17 && last >= 0 && last <= 348) {
                MayX.add(last);
            }
            last += cd;
        }
        return MayX;
    }

    public record Info(
            int cost,
            int tech,
            String name,
            int hp,
            int cd,
            int at,
            int shoot,
            float dmg
    ){};
}
