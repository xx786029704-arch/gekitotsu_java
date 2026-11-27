package org.example.tools;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * 只读工具：导出 Java StrictMath(fdlibm) 的常量与校准语料，供 Rust 逐位移植验证。
 *
 * <p>用法：
 * <pre>
 *   java --add-opens java.base/java.lang=ALL-UNNAMED \
 *        org.example.tools.MathDump consts fixtures/constants/fdlibm_consts.txt
 *   java --add-opens java.base/java.lang=ALL-UNNAMED \
 *        org.example.tools.MathDump corpus fixtures/math/math_corpus.bin 20000 20260924
 * </pre>
 *
 * <p>consts：反射 java.base 私有嵌套类 java.lang.FdLibm$*（需 --add-opens）与 java.lang.Math
 * 的确定性常量，输出原始位模式文本。
 *
 * <p>corpus：按固定种子生成 f32 输入域（系统扫描 + 边界值 + 随机样本）在引擎实际调用形态下的
 * StrictMath 结果，写成二进制；同时用 Math.* 复算并统计不一致数（写入同名 .info.txt）。
 * 记录格式（小端）：magic('GKTM') u32, version u32, count u32；随后每条 25 字节：
 * func u8, a u64, b u64, out u64（一参函数 b=0）。func: 0=sin 1=cos 2=toDegrees 3=toRadians 4=atan2。
 */
public final class MathDump {

    private static final int FUNC_SIN = 0;
    private static final int FUNC_COS = 1;
    private static final int FUNC_TO_DEGREES = 2;
    private static final int FUNC_TO_RADIANS = 3;
    private static final int FUNC_ATAN2 = 4;

    /** oracle 引擎中的 0.017453292519943295F（f32），Sector/EllipticalSector/SearchBullet 等共用 */
    public static final float DEG_TO_RAD_F32 = 0.017453292519943295F;

    private MathDump() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: MathDump consts <out> | corpus <out.bin> <randoms> <seed>");
            System.exit(2);
        }
        switch (args[0]) {
            case "consts" -> dumpConsts(args[1]);
            case "corpus" -> dumpCorpus(args[1], Integer.parseInt(args[2]), Long.parseLong(args[3]));
            default -> {
                System.err.println("unknown mode: " + args[0]);
                System.exit(2);
            }
        }
    }

    // ================= constants =================

    private static final String[] CONST_CLASSES = {
            "java.lang.FdLibm",
            "java.lang.FdLibm$Sin",
            "java.lang.FdLibm$Cos",
            "java.lang.FdLibm$RemPio2",
            "java.lang.FdLibm$KernelRemPio2",
            "java.lang.FdLibm$Atan",
            "java.lang.FdLibm$Atan2",
            "org.example.tools.MathDump",
    };

    private static void dumpConsts(String out) throws Exception {
        StringBuilder sb = new StringBuilder(1 << 16);
        for (String name : CONST_CLASSES) {
            dumpClass(sb, Class.forName(name));
        }
        dumpMathConstants(sb);
        Files.writeString(Path.of(out), sb.toString(), StandardCharsets.UTF_8);
    }

    private static void dumpMathConstants(StringBuilder sb) throws Exception {
        Class<?> c = Class.forName("java.lang.Math");
        sb.append("class java.lang.Math\n");
        List<Field> fs = new ArrayList<>();
        for (String keep : new String[]{"PI", "E", "DEGREES_TO_RADIANS", "RADIANS_TO_DEGREES"}) {
            fs.add(c.getDeclaredField(keep));
        }
        fs.sort(Comparator.comparing(Field::getName));
        for (Field f : fs) {
            f.setAccessible(true);
            if (f.getType() == double.class) {
                sb.append("f64 ").append(f.getName()).append(' ')
                        .append(hex(Double.doubleToRawLongBits(f.getDouble(null)), 16)).append('\n');
            }
        }
    }

    private static void dumpClass(StringBuilder sb, Class<?> c) throws Exception {
        sb.append("class ").append(c.getName()).append('\n');
        List<Field> fields = new ArrayList<>();
        for (Field f : c.getDeclaredFields()) {
            if (!f.isSynthetic() && Modifier.isStatic(f.getModifiers())) {
                fields.add(f);
            }
        }
        fields.sort(Comparator.comparing(Field::getName));
        for (Field f : fields) {
            f.setAccessible(true);
            String n = f.getName();
            Class<?> t = f.getType();
            if (t == double.class) {
                sb.append("f64 ").append(n).append(' ')
                        .append(hex(Double.doubleToRawLongBits(f.getDouble(null)), 16)).append('\n');
            } else if (t == float.class) {
                sb.append("f32 ").append(n).append(' ')
                        .append(hex(Float.floatToRawIntBits(f.getFloat(null)) & 0xffff_ffffL, 8)).append('\n');
            } else if (t == int.class) {
                sb.append("i32 ").append(n).append(' ').append(f.getInt(null)).append('\n');
            } else if (t == double[].class) {
                double[] a = (double[]) f.get(null);
                sb.append("f64[] ").append(n).append(' ').append(a.length).append('\n');
                for (int i = 0; i < a.length; i++) {
                    sb.append("f64 ").append(n).append('[').append(i).append("] ")
                            .append(hex(Double.doubleToRawLongBits(a[i]), 16)).append('\n');
                }
            } else if (t == int[].class) {
                int[] a = (int[]) f.get(null);
                sb.append("i32[] ").append(n).append(' ').append(a.length).append('\n');
                for (int i = 0; i < a.length; i++) {
                    sb.append("i32 ").append(n).append('[').append(i).append("] ").append(a[i]).append('\n');
                }
            }
        }
    }

    private static String hex(long bits, int width) {
        String s = Long.toHexString(bits);
        StringBuilder sb = new StringBuilder("0x");
        for (int i = s.length(); i < width; i++) {
            sb.append('0');
        }
        return sb.append(s).toString();
    }

    // ================= corpus =================

    private static ByteBuffer out;
    private static int count;
    private static int mathMismatch;
    private static final int[] mismatchByFunc = new int[8];
    private static final int[] mismatchByMag = new int[6];
    private static final StringBuilder mismatchSamples = new StringBuilder();

    private static void dumpCorpus(String path, int randomSamples, long seed) throws IOException {
        int capacity = 12 + (int) ((randomSamples * 17L + 40000L) * 25L * 2L);
        out = ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
        out.position(12);
        count = 0;
        mathMismatch = 0;
        java.util.Arrays.fill(mismatchByFunc, 0);
        java.util.Arrays.fill(mismatchByMag, 0);
        mismatchSamples.setLength(0);

        systematicSweep();
        boundarySweep();
        randomSweep(new Random(seed), randomSamples);

        out.putInt(0, 0x4D544B47);      // 'GKTM' little-endian
        out.putInt(4, 1);
        out.putInt(8, count);
        try (var ch = java.nio.channels.FileChannel.open(Path.of(path),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                java.nio.file.StandardOpenOption.WRITE)) {
            out.position(0);
            out.limit(12 + count * 25);
            ch.write(out);
        }
        String info = "records=" + count + "\nmath_mismatches=" + mathMismatch + "\n"
                + "mismatch_by_func(sin,cos,toDeg,toRad,atan2)=" + mismatchByFunc[0] + "," + mismatchByFunc[1]
                + "," + mismatchByFunc[2] + "," + mismatchByFunc[3] + "," + mismatchByFunc[4] + "\n"
                + "mismatch_by_|a|(<2^-20,<1,<7,<1e3,<1e7,>=1e7)=" + mismatchByMag[0] + "," + mismatchByMag[1]
                + "," + mismatchByMag[2] + "," + mismatchByMag[3] + "," + mismatchByMag[4] + "," + mismatchByMag[5] + "\n"
                + mismatchSamples
                + "DEG_TO_RAD_F32=0x" + Integer.toHexString(Float.floatToRawIntBits(DEG_TO_RAD_F32)) + "\n"
                + "jdk=" + System.getProperty("java.version") + "\n";
        Files.writeString(Path.of(path + ".info.txt"), info, StandardCharsets.UTF_8);
        System.out.print(info);
    }

    /** 系统扫描：指数 -24..6，覆盖尾数采样与两个符号。 */
    private static void systematicSweep() {
        for (int e = -24; e <= 6; e++) {
            for (int j = 0; j <= 255; j++) {
                int bits = ((e + 127) << 23) | (j << 15);
                float v = Float.intBitsToFloat(bits);
                if (!Float.isFinite(v)) {
                    continue;
                }
                pipeline(v);
                pipeline(-v);
            }
        }
    }

    /** 边界扫描：参数归约分支阈值附近；以及 0/次正规/极值/NaN/Inf。 */
    private static void boundarySweep() {
        double[] bases = {
                0.0, 1.0, -1.0, 0.5, -0.5,
                Math.PI / 4, -Math.PI / 4, Math.PI / 2, -Math.PI / 2,
                3 * Math.PI / 4, -3 * Math.PI / 4, 2 * Math.PI, -2 * Math.PI,
                Math.PI, -Math.PI,
                Math.scalb(1.0, 19) * Math.PI / 2,
                -Math.scalb(1.0, 19) * Math.PI / 2,
                Math.scalb(1.0, 20), -Math.scalb(1.0, 20),
                Math.scalb(1.0, 66), -Math.scalb(1.0, 66),
                Math.scalb(1.0, 500), -Math.scalb(1.0, 500),
                Double.MIN_VALUE, -Double.MIN_VALUE,
                Double.MIN_NORMAL, -Double.MIN_NORMAL,
                Double.MAX_VALUE, -Double.MAX_VALUE,
                Float.MAX_VALUE, -Float.MAX_VALUE,
                Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
        };
        for (double b : bases) {
            for (int d = -3; d <= 3; d++) {
                oneArgPipelines(shift(b, d));
            }
        }
        // n*pi/2 的邻域（覆盖归约 npio2_hw 分支）
        for (int n = 1; n <= 33; n++) {
            double v = n * Math.PI / 2;
            for (int d = -2; d <= 2; d++) {
                oneArgPipelines(shift(v, d));
            }
        }
    }

    private static double shift(double v, int d) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v == 0.0) {
            return d == 0 ? v : Math.copySign(Double.MIN_VALUE * Math.abs(d), v == 0.0 ? 1.0 : v);
        }
        double r = v;
        for (int i = 0; i < Math.abs(d); i++) {
            r = d > 0 ? Math.nextUp(r) : Math.nextDown(r);
        }
        return r;
    }

    private static void randomSweep(Random rng, int samples) {
        for (int i = 0; i < samples; i++) {
            float v = Float.intBitsToFloat(rng.nextInt());
            if (Float.isFinite(v)) {
                pipeline(v);
            }
            // atan2 输入对（引擎里是坐标差）
            float yf = Float.intBitsToFloat(rng.nextInt());
            float xf = Float.intBitsToFloat(rng.nextInt());
            if (!Float.isFinite(yf) || !Float.isFinite(xf)) {
                continue;
            }
            double dy = yf;
            double dx = xf;
            double a = StrictMath.atan2(dy, dx);
            rec(FUNC_ATAN2, dy, dx, a);
            // 引擎管线：atan2 → toDegrees → (float) → toRadians → sin/cos；以及 rot*DEG_TO_RAD_F32
            float rot = (float) StrictMath.toDegrees(a);
            double rad = StrictMath.toRadians(rot);
            rec(FUNC_SIN, rad, 0, StrictMath.sin(rad));
            rec(FUNC_COS, rad, 0, StrictMath.cos(rad));
            float m = rot * DEG_TO_RAD_F32;
            rec(FUNC_SIN, (double) m, 0, StrictMath.sin((double) m));
            rec(FUNC_COS, (double) m, 0, StrictMath.cos((double) m));
        }
    }

    private static void pipeline(float v) {
        oneArgPipelines(v);
        // Sector/EllipticalSector 构造：dir = direction * DEG_TO_RAD_F32（f32 运算后加宽）
        float dir = v * DEG_TO_RAD_F32;
        rec(FUNC_SIN, (double) dir, 0, StrictMath.sin((double) dir));
        rec(FUNC_COS, (double) dir, 0, StrictMath.cos((double) dir));
        // update()：a * 0.5f
        float half = v * 0.5f;
        rec(FUNC_SIN, (double) half, 0, StrictMath.sin((double) half));
        rec(FUNC_COS, (double) half, 0, StrictMath.cos((double) half));
        // Utils.cosF/sinF：(double)RDeg * 0.017453292519943295F
        double widen = (double) v * (double) DEG_TO_RAD_F32;
        rec(FUNC_SIN, widen, 0, StrictMath.sin(widen));
        rec(FUNC_COS, widen, 0, StrictMath.cos(widen));
    }

    private static void oneArgPipelines(double v) {
        rec(FUNC_SIN, v, 0, StrictMath.sin(v));
        rec(FUNC_COS, v, 0, StrictMath.cos(v));
        rec(FUNC_TO_DEGREES, v, 0, StrictMath.toDegrees(v));
        rec(FUNC_TO_RADIANS, v, 0, StrictMath.toRadians(v));
    }

    /** 记录严格结果；同时用 Math.* 复算并统计差异数。 */
    private static void rec(int func, double a, double b, double strictOut) {
        double mathOut = switch (func) {
            case FUNC_SIN -> Math.sin(a);
            case FUNC_COS -> Math.cos(a);
            case FUNC_TO_DEGREES -> Math.toDegrees(a);
            case FUNC_TO_RADIANS -> Math.toRadians(a);
            case FUNC_ATAN2 -> Math.atan2(a, b);
            default -> throw new IllegalArgumentException();
        };
        if (Double.doubleToRawLongBits(mathOut) != Double.doubleToRawLongBits(strictOut)) {
            mathMismatch++;
            mismatchByFunc[func]++;
            double mag = Math.abs(a);
            mismatchByMag[mag < 0x1p-20 ? 0 : mag < 1 ? 1 : mag < 7 ? 2 : mag < 1e3 ? 3 : mag < 1e7 ? 4 : 5]++;
            if (mismatchSamples.length() < 2000) {
                mismatchSamples.append("mismatch func=").append(func)
                        .append(" a=0x").append(Long.toHexString(Double.doubleToRawLongBits(a)))
                        .append(" b=0x").append(Long.toHexString(Double.doubleToRawLongBits(b)))
                        .append(" strict=0x").append(Long.toHexString(Double.doubleToRawLongBits(strictOut)))
                        .append(" math=0x").append(Long.toHexString(Double.doubleToRawLongBits(mathOut)))
                        .append('\n');
            }
        }
        ensure(count + 2);
        out.put((byte) func);
        out.putLong(Double.doubleToRawLongBits(a));
        out.putLong(Double.doubleToRawLongBits(b));
        out.putLong(Double.doubleToRawLongBits(strictOut));
        count++;
    }

    private static void ensure(int records) {
        if (12 + records * 25 > out.capacity()) {
            int newCap = out.capacity() * 2;
            ByteBuffer bigger = ByteBuffer.allocate(newCap).order(ByteOrder.LITTLE_ENDIAN);
            bigger.put(0, out, 0, 12 + count * 25);
            out = bigger;
        }
    }
}
