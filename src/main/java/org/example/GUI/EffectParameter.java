package org.example.GUI;

/** 效果参数定义。 */
public record EffectParameter(
        String key,
        String label,
        Type type,
        Object defaultValue
) {
    public enum Type { INT, STRING, BOOLEAN, UNIT_ID }
}
