package org.example.GUI;


import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Trace implements ListItem {
    public Color color;
    public boolean isNear;
    public boolean isBallFirst;
    public String x;
    public String y;
    public String wall_x;
    public String speed0;
    public String speed1;
    public String times;
    public boolean visible = true;
    public String name;

    public Trace(String X, String Y, String WX, String speed0, String speed1, String times,
                 boolean IsNear, boolean IsBallFirst, Color C, String Name) {
        super();
        this.x = X;
        this.y = Y;
        this.wall_x = WX;
        this.speed0 = speed0;
        this.speed1 = speed1;
        this.isNear = IsNear;
        this.isBallFirst = IsBallFirst;
        this.color = C;
        this.times = times;
        this.name = Name;
    }

    // --- 表达式求值（带边界钳制） ---

    public int evalX(Map<String, Integer> vars) {
        return Math.clamp(ExprEvaluator.eval(x, vars), 0, 348);
    }

    public int evalY(Map<String, Integer> vars) {
        return Math.clamp(ExprEvaluator.eval(y, vars), -33, 349);
    }

    public int evalWallX(Map<String, Integer> vars) {
        return Math.clamp(ExprEvaluator.eval(wall_x, vars), 0, 348);
    }

    public int evalSpeed0(Map<String, Integer> vars) {
        return Math.clamp(ExprEvaluator.eval(speed0, vars), 0, 15);
    }

    public int evalSpeed1(Map<String, Integer> vars) {
        return Math.clamp(ExprEvaluator.eval(speed1, vars), 0, 15);
    }

    public int evalTimes(Map<String, Integer> vars) {
        return Math.clamp(ExprEvaluator.eval(times, vars), 1, 16);
    }

    /** 将变量列表转为求值用的 name→val 映射。 */
    public static Map<String, Integer> varMap(List<Variable> variables) {
        Map<String, Integer> map = new HashMap<>();
        for (Variable v : variables) map.put(v.name, v.val);
        return map;
    }

    /** 序列化为人类可读文本，用于剪贴板复制。 */
    public String toHumanReadable() {
        StringBuilder sb = new StringBuilder();
        sb.append(org.example.I18n.t("field.traceName")).append(": ").append(name).append("\n");
        sb.append(org.example.I18n.t("field.ballX")).append(": ").append(x).append("\n");
        sb.append(org.example.I18n.t("field.ballY")).append(": ").append(y).append("\n");
        sb.append(org.example.I18n.t("field.wallX")).append(": ").append(wall_x).append("\n");
        sb.append(org.example.I18n.t("field.accel0")).append(": ").append(speed0).append("\n");
        sb.append(org.example.I18n.t("field.accel1")).append(": ").append(speed1).append("\n");
        sb.append(org.example.I18n.t("field.stages")).append(": ").append(times).append("\n");
        sb.append(org.example.I18n.t("field.isNear")).append(": ").append(isNear).append("\n");
        sb.append(org.example.I18n.t("field.ballFirst")).append(": ").append(isBallFirst).append("\n");
        sb.append(org.example.I18n.t("field.color")).append(": ").append(colorToHex(color));
        return sb.toString();
    }

    /** 从人类可读文本反序列化（字段名兼容中日英三种语言）。解析失败返回 null。 */
    public static Trace fromHumanReadable(String text) {
        try {
            Map<String, String> map = new HashMap<>();
            for (String line : text.split("\n")) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                int idx = trimmed.indexOf(": ");
                if (idx > 0) {
                    map.put(trimmed.substring(0, idx), trimmed.substring(idx + 2));
                }
            }
            String name = org.example.I18n.pick(map, "field.traceName");
            if (name == null) return null;

            String x = org.example.I18n.pick(map, "field.ballX", "0");
            String y = org.example.I18n.pick(map, "field.ballY", "0");
            String wx = org.example.I18n.pick(map, "field.wallX", "0");
            String sp0 = org.example.I18n.pick(map, "field.accel0", "0");
            String sp1 = org.example.I18n.pick(map, "field.accel1", "0");
            String times = org.example.I18n.pick(map, "field.stages", "1");
            boolean isNear = Boolean.parseBoolean(org.example.I18n.pick(map, "field.isNear", "false"));
            boolean isBallFirst = Boolean.parseBoolean(org.example.I18n.pick(map, "field.ballFirst", "false"));
            Color color = Color.decode(org.example.I18n.pick(map, "field.color", "#E03E3E"));

            return new Trace(x, y, wx, sp0, sp1, times, isNear, isBallFirst, color, name);
        } catch (Exception e) {
            return null;
        }
    }

    /** 将多个轨迹序列化，以 "---" 分隔。 */
    public static String serializeMultiple(List<Trace> traces) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < traces.size(); i++) {
            if (i > 0) sb.append("\n---\n");
            sb.append(traces.get(i).toHumanReadable());
        }
        return sb.toString();
    }

    /** 从多轨迹文本反序列化。兼容 \r\n 行尾。 */
    public static List<Trace> deserializeMultiple(String text) {
        text = text.replace("\r\n", "\n").replace('\r', '\n');
        List<Trace> result = new ArrayList<>();
        for (String block : text.split("\n---\n")) {
            String trimmed = block.replaceAll("^---\n?", "").replaceAll("\n?---$", "").trim();
            if (trimmed.isEmpty()) continue;
            Trace t = fromHumanReadable(trimmed);
            if (t != null) result.add(t);
        }
        return result;
    }

    private static String colorToHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    // --- ListItem 接口实现 ---
    @Override public String getName() { return name; }
    @Override public void setName(String name) { this.name = name; }
    @Override public Color getColor() { return color; }
    @Override public void setColor(Color color) { this.color = color; }
    @Override public boolean isVisible() { return visible; }
    @Override public void setVisible(boolean visible) { this.visible = visible; }
}
