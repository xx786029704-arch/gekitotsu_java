package org.example.GUI;

import org.example.Main;
import org.example.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 阵容数据对象，包含阵名和单位列表（第一个单位固定为核心）。 */
public class Formation {
    public String name;
    public final List<Unit> units;

    public Formation(String name, List<Unit> units) {
        this.name = name;
        this.units = new ArrayList<>(units);
    }

    /** 将阵容重新编码为 name&code 格式的字符串。 */
    public String encode() {
        StringBuilder sb = new StringBuilder();
        for (Unit u : units) {
            String e = u.encode();
            if (!e.isEmpty()) {
                sb.append(e);
            }
        }
        String code = sb.toString();
        if (name == null || name.isEmpty()) {
            return code;
        }
        return name + "&" + code + encodeHp();
    }

    public String encodeHp(){
        StringBuilder sb = new StringBuilder("#");
        int[] hps = {239, 239, 239};
        int n = units.size();
        boolean flg = true;
        for (int i = 0; i < n; i++) {
            Unit u = units.get(i);

            if (u.hp < Unit.infos[u.id].hp()){
                flg = false;
            }
            hps[i % 3] = u.hp;
            if (i % 3 == 2 || i == n - 1) {
                sb.append(Utils.to_rad61(hps[0]*57600 + hps[1]*240 + hps[2], 4));
                if (i % 3 == 2){
                    Arrays.fill(hps, 239);
                }
            }
        }
        return flg ? "" : sb.toString();
    }

    public Formation decodeHp(String str){
        int tmp = 0;
        int n = units.size();
        for(int i = 0; i < n; i++){
            Unit u = units.get(i);
            if (i % 3 == 0){
                if (str.length() < 4) return this;
                tmp = Main.pskey.indexOf(str.charAt(0)) * 226981
                        + Main.pskey.indexOf(str.charAt(1)) * 3721
                        + Main.pskey.indexOf(str.charAt(2)) * 61
                        + Main.pskey.indexOf(str.charAt(3));
                u.hp = tmp / 57600;
                tmp %= 57600;
                str = str.substring(4);
            } else if (i % 3 == 1) {
                u.hp = tmp / 240;
                tmp %= 240;
            } else {
                u.hp = tmp;
            }
        }
        return this;
    }

    public static List<Unit> decodeUnits(String code){
        Unit core = Unit.decodeAsCore(code.substring(0, 6));
        List<Unit> units = new ArrayList<>();
        units.add(core);
        int unitCount = code.length() / 6 - 1;
        for (int i = 0; i < unitCount; i++) {
            int start = 6 + i * 6;
            Unit unit = Unit.decode(code.substring(start, start + 6));
            if (unit.id >= 0 && unit.id < Unit.infos.length) {
                units.add(unit);
            }
        }
        return units;
    }

    public static Formation decode(String text){
        String name, code, hpCode;
        int ampIdx = text.indexOf('&');
        if (ampIdx >= 0) {
            name = text.substring(0, ampIdx).trim();
            code = text.substring(ampIdx + 1).trim();
        } else {
            name = "";
            code = text;
        }
        ampIdx = code.indexOf('#');
        if (ampIdx >= 0) {
            hpCode = code.substring(ampIdx + 1).trim();
            code = code.substring(0, ampIdx).trim();
        } else {
            hpCode = "";
        }
        name = name.replace("/", "");
        code = code.replaceAll("[^A-Za-z0-9]", "");
        if (code.length() % 6 != 0) {
            throw new IllegalArgumentException("代码长度错误");
        }
        return new Formation(name, decodeUnits(code)).decodeHp(hpCode);
    }

    public int getAccelLevel(){
        int accelLevel = 0;
        for (Unit u : units) {
            if (u.id == 29) accelLevel++;
            else if (u.id == 30) accelLevel+=2;
        }
        return accelLevel;
    }
}
