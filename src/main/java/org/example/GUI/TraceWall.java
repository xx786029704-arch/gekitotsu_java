package org.example.GUI;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraceWall implements ListItem {
    public String x;
    public String y;
    public String name;
    public boolean visible = true;
    public Color color;
    public boolean isCore;

    public TraceWall(String x, String y, String name, boolean visible, Color color, boolean isCore) {
        this.x = x;
        this.y = y;
        this.name = name;
        this.visible = visible;
        this.color = color;
        this.isCore = isCore;
    }

    // --- 表达式求值（带边界钳制） ---

    public int evalX(Map<String, Integer> vars) {
        return Math.clamp(ExprEvaluator.eval(x, vars), 0, isCore ? 276 : 348);
    }

    public int evalY(Map<String, Integer> vars) {
        return Math.clamp(ExprEvaluator.eval(y, vars), 0, isCore ? 276 : 349);
    }

    /** 序列化为人类可读文本，用于剪贴板复制。 */
    public String toHumanReadable() {
        StringBuilder sb = new StringBuilder();
        sb.append("要塞壁名称: ").append(name).append("\n");
        sb.append("x: ").append(x).append("\n");
        sb.append("y: ").append(y).append("\n");
        sb.append("颜色: ").append(colorToHex(color)).append("\n");
        sb.append("核心: ").append(isCore).append("\n");
        sb.append("可见: ").append(visible);
        return sb.toString();
    }

    /** 从人类可读文本反序列化。解析失败返回 null。 */
    public static TraceWall fromHumanReadable(String text) {
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
            if (!map.containsKey("要塞壁名称")) return null;

            String name = map.getOrDefault("要塞壁名称", "要塞壁");
            String x = map.getOrDefault("x", "0");
            String y = map.getOrDefault("y", "0");
            Color color = Color.decode(map.getOrDefault("颜色", "#E03E3E"));
            boolean isCore = Boolean.parseBoolean(map.getOrDefault("核心", "false"));
            boolean visible = Boolean.parseBoolean(map.getOrDefault("可见", "true"));

            return new TraceWall(x, y, name, visible, color, isCore);
        } catch (Exception e) {
            return null;
        }
    }

    /** 将多个要塞壁序列化，以 "---" 分隔。 */
    public static String serializeMultiple(List<TraceWall> walls) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < walls.size(); i++) {
            if (i > 0) sb.append("\n---\n");
            sb.append(walls.get(i).toHumanReadable());
        }
        return sb.toString();
    }

    /** 从多要塞壁文本反序列化。兼容 \r\n 行尾。 */
    public static List<TraceWall> deserializeMultiple(String text) {
        text = text.replace("\r\n", "\n").replace('\r', '\n');
        List<TraceWall> result = new ArrayList<>();
        for (String block : text.split("\n---\n")) {
            String trimmed = block.replaceAll("^---\n?", "").replaceAll("\n?---$", "").trim();
            if (trimmed.isEmpty()) continue;
            TraceWall w = fromHumanReadable(trimmed);
            if (w != null) result.add(w);
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
