package org.example;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 多语言支持。全部界面文案存于资源 /lang.json，结构为
 * { "zh": {key: text}, "ja": {...}, "en": {...} }。
 * 缺词条时回退中文，再缺则返回 key 本身，便于开发期发现遗漏。
 */
public final class I18n {
    public static final String ZH = "zh";
    public static final String JA = "ja";
    public static final String EN = "en";

    private static final Map<String, Map<String, String>> BUNDLES = new HashMap<>();
    private static boolean loaded = false;

    /** 当前语言代码，取值 zh / ja / en。 */
    public static String lang = ZH;

    private I18n() {}

    /** 设置当前语言（非法值忽略）。由启动流程与语言菜单调用。 */
    public static void setLang(String code) {
        if (ZH.equals(code) || JA.equals(code) || EN.equals(code)) {
            lang = code;
        }
    }

    /** 未配置语言时按系统语言推断，无法识别则回退中文。 */
    public static String detectSystemLang() {
        String l = java.util.Locale.getDefault().getLanguage();
        if ("ja".equals(l)) return JA;
        if ("en".equals(l)) return EN;
        return ZH;
    }

    private static synchronized void load() {
        if (loaded) return;
        loaded = true;
        try (InputStream in = I18n.class.getResourceAsStream("/lang.json")) {
            if (in == null) return;
            JsonNode root = new ObjectMapper().readTree(in);
            root.fields().forEachRemaining(entry -> {
                Map<String, String> map = new HashMap<>();
                entry.getValue().fields().forEachRemaining(f -> map.put(f.getKey(), f.getValue().asText()));
                BUNDLES.put(entry.getKey(), map);
            });
        } catch (Exception ignored) {}
    }

    /** 翻译 key。 */
    public static String t(String key) {
        load();
        String s = BUNDLES.getOrDefault(lang, Map.of()).get(key);
        if (s == null) s = BUNDLES.getOrDefault(ZH, Map.of()).get(key);
        return s != null ? s : key;
    }

    /** 翻译并格式化（文案中可用 %s / %d 占位）。 */
    public static String t(String key, Object... args) {
        String s = t(key);
        return args.length == 0 ? s : String.format(s, args);
    }

    /**
     * 在“字段名 → 值”映射中按 当前语言 → 中文 → 日文 → 英文 的顺序查找 key 对应字段。
     * 用于剪贴板/文本序列化格式的跨语言解析。
     */
    public static String pick(Map<String, String> map, String key) {
        load();
        for (String l : new String[]{lang, ZH, JA, EN}) {
            String label = BUNDLES.getOrDefault(l, Map.of()).get(key);
            if (label != null && map.containsKey(label)) {
                return map.get(label);
            }
        }
        return null;
    }

    /** 同 {@link #pick(Map, String)}，缺失时返回默认值。 */
    public static String pick(Map<String, String> map, String key, String def) {
        String v = pick(map, key);
        return v != null ? v : def;
    }

    /** 单位名称；词条缺失时回退 Unit.Info 内置中文名。 */
    public static String unitName(int id) {
        String key = "unit." + id + ".name";
        String s = t(key);
        if (s.equals(key)) {
            try {
                return org.example.GUI.Unit.infos[id].name();
            } catch (Exception ignored) {
                return key;
            }
        }
        return s;
    }

    /** 当前语言的 UI 字体族：中文=黑体，日文=Meiryo，英文=Segoe UI（缺失时由 Java 自动回退）。 */
    public static String fontFamily() {
        return switch (lang) {
            case JA -> "Meiryo";
            case EN -> "Segoe UI";
            default -> "黑体";
        };
    }

    /** 按当前语言创建 UI 字体。 */
    public static java.awt.Font font(int style, int size) {
        return new java.awt.Font(fontFamily(), style, size);
    }
}
