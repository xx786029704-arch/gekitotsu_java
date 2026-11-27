package org.example.tools;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

/**
 * 只读常量导出器（gekitotsu_ultra_fast 专用，不属于引擎）。
 *
 * <p>用反射把指定类中全部 static 字段（含 private）导出为原始位模式文本，供
 * tools/gensrc/gen_constants.py 生成 Rust 常量。不会写入 oracle 任何状态，
 * 也不改变引擎行为；但读取非编译期常量的静态字段会触发类初始化，因此类名必须显式指定。
 *
 * <p>用法：{@code java org.example.tools.ConstDump <out.txt> <class> [class...]}
 *
 * <p>输出格式（UTF-8、LF，字段名升序、按类分组）：
 * <pre>
 * class org.example.Utils
 * i32 DEFAULT_SEED 1
 * f32 trigonometricLookup[0] 0x00000000
 * f32[] trigonometricLookup 487
 * ...
 * </pre>
 * 浮点一律十六进制原始位（f32 8 位、f64 16 位）；String 为 UTF-8 的 Base64。
 */
public final class ConstDump {

    private ConstDump() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: ConstDump <out.txt> <class> [class...]");
            System.exit(2);
        }
        StringBuilder sb = new StringBuilder(1 << 20);
        for (int i = 1; i < args.length; i++) {
            dumpClass(sb, Class.forName(args[i]));
        }
        Path out = Path.of(args[0]);
        Path parent = out.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
    }

    private static void dumpClass(StringBuilder sb, Class<?> c) {
        sb.append("class ").append(c.getName()).append('\n');
        List<Field> fields = new ArrayList<>();
        for (Field f : c.getDeclaredFields()) {
            if (!f.isSynthetic() && Modifier.isStatic(f.getModifiers())) {
                fields.add(f);
            }
        }
        fields.sort(Comparator.comparing(Field::getName));
        for (Field f : fields) {
            String n = f.getName();
            Class<?> t = f.getType();
            try {
                f.setAccessible(true);
                if (t == int.class) {
                    line(sb, "i32", n, Integer.toString(f.getInt(null)));
                } else if (t == long.class) {
                    line(sb, "i64", n, Long.toString(f.getLong(null)));
                } else if (t == short.class) {
                    line(sb, "i16", n, Short.toString(f.getShort(null)));
                } else if (t == byte.class) {
                    line(sb, "i8", n, Byte.toString(f.getByte(null)));
                } else if (t == char.class) {
                    line(sb, "u16", n, Integer.toString(f.getChar(null)));
                } else if (t == float.class) {
                    line(sb, "f32", n, hex(Float.floatToRawIntBits(f.getFloat(null)), 8));
                } else if (t == double.class) {
                    line(sb, "f64", n, hex(Double.doubleToRawLongBits(f.getDouble(null)), 16));
                } else if (t == boolean.class) {
                    line(sb, "bool", n, Boolean.toString(f.getBoolean(null)));
                } else if (t == String.class) {
                    line(sb, "str", n, Base64.getEncoder()
                            .encodeToString(((String) f.get(null)).getBytes(StandardCharsets.UTF_8)));
                } else if (t.isArray() && t.getComponentType().isPrimitive()) {
                    Object a = f.get(null);
                    int len = Array.getLength(a);
                    Class<?> ct = t.getComponentType();
                    String tag = primTag(ct);
                    sb.append(tag).append("[] ").append(n).append(' ').append(len).append('\n');
                    for (int i = 0; i < len; i++) {
                        sb.append(tag).append(' ').append(n).append('[').append(i).append("] ")
                                .append(elem(ct, Array.get(a, i))).append('\n');
                    }
                }
            } catch (Throwable ex) {
                line(sb, "!error", n, ex.getClass().getSimpleName());
            }
        }
    }

    private static String primTag(Class<?> ct) {
        if (ct == int.class) return "i32";
        if (ct == long.class) return "i64";
        if (ct == short.class) return "i16";
        if (ct == byte.class) return "i8";
        if (ct == char.class) return "u16";
        if (ct == float.class) return "f32";
        if (ct == double.class) return "f64";
        if (ct == boolean.class) return "bool";
        throw new IllegalArgumentException("unsupported " + ct);
    }

    private static String elem(Class<?> ct, Object v) {
        if (ct == float.class) return hex(Float.floatToRawIntBits((Float) v), 8);
        if (ct == double.class) return hex(Double.doubleToRawLongBits((Double) v), 16);
        if (ct == boolean.class) return v.toString();
        if (ct == char.class) return Integer.toString((Character) v);
        return v.toString();
    }

    private static String hex(long bits, int width) {
        long mask = width >= 16 ? -1L : (1L << (width * 4)) - 1;
        String s = Long.toHexString(bits & mask);
        StringBuilder sb = new StringBuilder("0x");
        for (int i = s.length(); i < width; i++) {
            sb.append('0');
        }
        return sb.append(s).toString();
    }

    private static void line(StringBuilder sb, String tag, String name, String value) {
        sb.append(tag).append(' ').append(name).append(' ').append(value).append('\n');
    }
}
