package org.example.GUI;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 变量数据模型：单字母变量名 + 上下限 + 当前值。实现 ListItem 以复用列表 UI。 */
public class Variable implements ListItem {
    public String name;
    public int sub;
    public int inf;
    public int val;

    public Variable(String name, int sub, int inf, int val) {
        this.name = name;
        this.sub = sub;
        this.inf = inf;
        this.val = val;
    }

    /** 序列化为人类可读文本，用于剪贴板复制。 */
    public String toHumanReadable() {
        StringBuilder sb = new StringBuilder();
        sb.append("变量名称: ").append(name).append("\n");
        sb.append("最大值: ").append(sub).append("\n");
        sb.append("最小值: ").append(inf).append("\n");
        sb.append("当前值: ").append(val);
        return sb.toString();
    }

    /** 从人类可读文本反序列化。解析失败返回 null。 */
    public static Variable fromHumanReadable(String text) {
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
            if (!map.containsKey("变量名称")) return null;

            String name = map.getOrDefault("变量名称", "x");
            int sub = Integer.parseInt(map.getOrDefault("最大值", "100"));
            int inf = Integer.parseInt(map.getOrDefault("最小值", "0"));
            int val = Integer.parseInt(map.getOrDefault("当前值", "0"));

            return new Variable(name, sub, inf, val);
        } catch (Exception e) {
            return null;
        }
    }

    /** 将多个变量序列化，以 "---" 分隔。 */
    public static String serializeMultiple(List<Variable> vars) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vars.size(); i++) {
            if (i > 0) sb.append("\n---\n");
            sb.append(vars.get(i).toHumanReadable());
        }
        return sb.toString();
    }

    /** 从多变量文本反序列化。兼容 \r\n 行尾。 */
    public static List<Variable> deserializeMultiple(String text) {
        text = text.replace("\r\n", "\n").replace('\r', '\n');
        List<Variable> result = new ArrayList<>();
        for (String block : text.split("\n---\n")) {
            String trimmed = block.replaceAll("^---\n?", "").replaceAll("\n?---$", "").trim();
            if (trimmed.isEmpty()) continue;
            Variable v = fromHumanReadable(trimmed);
            if (v != null) result.add(v);
        }
        return result;
    }

    /** 返回当前已用名称集合中首个未被使用的小写字母 (a-z)，全部用满则返回 null。 */
    public static String firstUnusedLetter(Set<String> usedNames) {
        for (char c = 'a'; c <= 'z'; c++) {
            if (!usedNames.contains(String.valueOf(c))) return String.valueOf(c);
        }
        return null;
    }

    // --- ListItem 接口实现 ---
    @Override public String getName() { return name; }
    @Override public void setName(String name) { this.name = name; }
    @Override public Color getColor() { return null; }
    @Override public void setColor(Color color) {}
    @Override public boolean isVisible() { return true; }
    @Override public void setVisible(boolean visible) {}
}
