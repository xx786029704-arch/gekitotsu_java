package org.example.tools;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 只读工具：把浮点字面量文本（每行 {@code f32 <text>} 或 {@code f64 <text>}）解析为原始位。
 *
 * <p>用法：{@code java org.example.tools.LitDump <in.list> <out.txt>}
 *
 * <p>输出行：{@code <kind> <规范化文本> <0xhex>}。Java 的 {@code Float/Double.parseFloat/parseDouble}
 * 与编译期字面量转换同为正确舍入，故可代表源码中的字面量值。
 */
public final class LitDump {

    private LitDump() {
    }

    public static void main(String[] args) throws Exception {
        List<String> lines = Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder(1 << 16);
        for (String line : lines) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            int sp = t.indexOf(' ');
            String kind = t.substring(0, sp);
            String text = t.substring(sp + 1).trim();
            if (kind.equals("f32")) {
                float v = Float.parseFloat(text);
                sb.append("f32 ").append(text).append(' ').append("0x")
                        .append(pad(Integer.toHexString(Float.floatToRawIntBits(v)), 8)).append('\n');
            } else if (kind.equals("f64")) {
                double v = Double.parseDouble(text);
                sb.append("f64 ").append(text).append(' ').append("0x")
                        .append(pad(Long.toHexString(Double.doubleToRawLongBits(v)), 16)).append('\n');
            } else {
                throw new IllegalArgumentException("unknown kind: " + kind);
            }
        }
        Files.writeString(Path.of(args[1]), sb.toString(), StandardCharsets.UTF_8);
    }

    private static String pad(String s, int width) {
        StringBuilder sb = new StringBuilder();
        for (int i = s.length(); i < width; i++) {
            sb.append('0');
        }
        return sb.append(s).toString();
    }
}
