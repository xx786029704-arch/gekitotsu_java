package org.example.tools;

import org.example.CompiledFort;
import org.example.EllipticalSector;
import org.example.Fort;
import org.example.Main;
import org.example.Polygon;
import org.example.Rectangle;
import org.example.Round;
import org.example.Sector;
import org.example.CompositeShape;
import org.example.Shape;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

/**
 * 只读金样导出：几何基元（点碰撞/构造字段）与 fort code 解码，供 Rust 逐位对拍。
 *
 * <p>用法：
 * <pre>
 *   java -cp ... org.example.tools.GeoDump shapes fixtures/golden/geometry.txt
 *   java -cp ... org.example.tools.GeoDump forts  fixtures/golden/forts.txt fixtures/games/1P.txt fixtures/games/2P.txt
 * </pre>
 *
 * <p>所有浮点一律十六进制原始位（0x + 8 位）。格式见 Rust 侧 tests/l1_geom.rs、tests/l1_decode.rs。
 */
public final class GeoDump {

    private static final long SEED = 20260924L;
    private static final StringBuilder SB = new StringBuilder(1 << 20);

    private GeoDump() {
    }

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "shapes" -> shapes(args[1]);
            case "forts" -> forts(args[1], Arrays.copyOfRange(args, 2, args.length));
            default -> {
                System.err.println("unknown mode: " + args[0]);
                System.exit(2);
            }
        }
    }

    // ================= shapes =================

    private static void shapes(String outPath) throws Exception {
        Random rng = new Random(SEED);
        for (int i = 0; i < 40; i++) {
            Round round = new Round(rv(rng, 2000f), rv(rng, 2000f), Math.abs(rv(rng, 200f)) + 0.25f);
            dumpRound(round, rng);
        }
        for (int i = 0; i < 40; i++) {
            Rectangle rect = new Rectangle(rv(rng, 2000f), rv(rng, 2000f),
                    rv(rng, 100f), rv(rng, 100f), Math.abs(rv(rng, 300f)), Math.abs(rv(rng, 300f)));
            dumpRectangle(rect, rng);
        }
        for (int i = 0; i < 40; i++) {
            int n = 3 + rng.nextInt(5);
            float[][] vs = new float[n][2];
            for (int k = 0; k < n; k++) {
                vs[k][0] = rv(rng, 100f);
                vs[k][1] = rv(rng, 100f);
            }
            dumpPolygon(new Polygon(rv(rng, 2000f), rv(rng, 2000f), vs), rng);
        }
        for (int i = 0; i < 40; i++) {
            float angle = rv(rng, 720f);
            float direction = rv(rng, 720f);
            dumpSector(new Sector(rv(rng, 2000f), rv(rng, 2000f), Math.abs(rv(rng, 300f)),
                    angle, direction), angle, direction, rng);
        }
        for (int i = 0; i < 40; i++) {
            float angle = rv(rng, 720f);
            float direction = rv(rng, 720f);
            float flatness = rv(rng, 4f);
            dumpEllipticalSector(new EllipticalSector(rv(rng, 2000f), rv(rng, 2000f),
                    Math.abs(rv(rng, 300f)), angle, direction, flatness), angle, direction, flatness, rng);
        }
        for (int i = 0; i < 20; i++) {
            CompositeShape c = new CompositeShape(rv(rng, 500f), rv(rng, 500f));
            int sub = 1 + rng.nextInt(3);
            for (int k = 0; k < sub; k++) {
                c.addShape(new Round(rv(rng, 300f), rv(rng, 300f), Math.abs(rv(rng, 80f)) + 1f));
            }
            c.addShape(new Polygon(rv(rng, 200f), rv(rng, 200f), new float[][]{{0f, 0f}, {20f, 0f}, {0f, 20f}}));
            dumpComposite(c, rng);
        }
        // 嵌套复合
        for (int i = 0; i < 10; i++) {
            CompositeShape c = new CompositeShape(rv(rng, 200f), rv(rng, 200f));
            CompositeShape inner = new CompositeShape(rv(rng, 100f), rv(rng, 100f));
            inner.addShape(new Round(rv(rng, 50f), rv(rng, 50f), Math.abs(rv(rng, 20f)) + 1f));
            c.addShape(inner);
            dumpComposite(c, rng);
        }
        Files.writeString(Path.of(outPath), SB.toString(), StandardCharsets.UTF_8);
        System.out.println("shapes written to " + outPath);
    }

    private static void dumpRound(Round s, Random rng) {
        top("round", s.x, s.y, s.r);
        dumpPoints(s, rng, s.x, s.y, s.r);
    }

    private static void dumpRectangle(Rectangle s, Random rng) {
        top("rectangle", s.x, s.y, startX(s), startY(s), width(s), height(s));
        dumpPoints(s, rng, s.x, s.y, Math.max(width(s), height(s)) + Math.abs(startX(s)) + Math.abs(startY(s)) + 1f);
    }

    private static void dumpPolygon(Polygon s, Random rng) {
        SB.append("shape polygon ").append(b(s.x)).append(' ').append(b(s.y)).append(' ')
                .append(s.localVertices.length);
        for (float[] v : s.localVertices) {
            SB.append(' ').append(b(v[0])).append(' ').append(b(v[1]));
        }
        SB.append('\n');
        line("fields", b(field(s, "minX")), b(field(s, "maxX")), b(field(s, "minY")), b(field(s, "maxY")));
        dumpPoints(s, rng, s.x, s.y, 150f);
    }

    private static void dumpSector(Sector s, float angle, float direction, Random rng) {
        top("sector", s.x, s.y, s.r, angle, direction);
        line("fields", b(s.dirX), b(s.dirY), b(s.cosHalfAngle), b(s.a), b(s.dir));
        dumpPoints(s, rng, s.x, s.y, s.r);
    }

    private static void dumpEllipticalSector(EllipticalSector s, float angle, float direction, float flatness, Random rng) {
        top("esector", s.x, s.y, s.r, angle, direction, flatness);
        line("fields", b(s.dirX), b(s.dirY), b(s.cosHalfAngle), b(s.a), b(s.dir), b(s.flatness));
        dumpPoints(s, rng, s.x, s.y, s.r);
    }

    private static void dumpComposite(CompositeShape s, Random rng) {
        SB.append("shape composite ").append(b(s.x)).append(' ').append(b(s.y)).append(' ')
                .append(s.getShapes().size()).append('\n');
        for (Shape sub : s.getShapes()) {
            dumpSub(sub, rng);
        }
        dumpPoints(s, rng, s.x, s.y, 400f);
    }

    private static void dumpSub(Shape s, Random rng) {
        if (s instanceof Round r) {
            SB.append("sub round ").append(b(r.x)).append(' ').append(b(r.y)).append(' ').append(b(r.r)).append('\n');
        } else if (s instanceof Polygon p) {
            SB.append("sub polygon ").append(b(p.x)).append(' ').append(b(p.y)).append(' ')
                    .append(p.localVertices.length);
            for (float[] v : p.localVertices) {
                SB.append(' ').append(b(v[0])).append(' ').append(b(v[1]));
            }
            SB.append('\n');
        } else if (s instanceof CompositeShape c) {
            SB.append("sub composite ").append(b(c.x)).append(' ').append(b(c.y)).append(' ')
                    .append(c.getShapes().size()).append('\n');
            for (Shape inner : c.getShapes()) {
                dumpSub(inner, rng);
            }
        } else {
            throw new IllegalStateException("unsupported sub shape " + s);
        }
    }

    private static void dumpPoints(Shape s, Random rng, float cx, float cy, float span) {
        for (int i = 0; i < 25; i++) {
            float px = cx + rv(rng, span);
            float py = cy + rv(rng, span);
            line("p", b(px), b(py), s.hitTestPoint(px, py) ? "1" : "0");
        }
        // 特殊点：NaN / ±Inf / -0 / 极值 / 形状中心 / 边界
        float[] specialX = {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -0.0f,
                Float.MAX_VALUE, Float.MIN_VALUE, cx, cx + span, cx - span};
        float[] specialY = {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -0.0f,
                Float.MAX_VALUE, Float.MIN_VALUE, cy, cy + span, cy - span};
        for (int i = 0; i < specialX.length; i++) {
            line("p", b(specialX[i]), b(specialY[i]), s.hitTestPoint(specialX[i], specialY[i]) ? "1" : "0");
        }
    }

    private static void top(String kind, float... vals) {
        SB.append("shape ").append(kind);
        for (float v : vals) {
            SB.append(' ').append(b(v));
        }
        SB.append('\n');
    }

    private static void line(String tag, String... toks) {
        SB.append(tag);
        for (String t : toks) {
            SB.append(' ').append(t);
        }
        SB.append('\n');
    }

    private static String b(float v) {
        return "0x" + Integer.toHexString(Float.floatToRawIntBits(v));
    }

    private static int iField(Object o, String name) {
        try {
            var f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.getInt(o);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static int[] iArray(Object o, String name) {
        try {
            var f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return (int[]) f.get(o);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /** 随机 float，可为负、可为 0，含边界。 */
    private static float rv(Random rng, float scale) {
        int mode = rng.nextInt(8);
        if (mode == 0) {
            return 0f;
        }
        if (mode == 1) {
            return -0.0f;
        }
        return (rng.nextFloat() * 2f - 1f) * scale;
    }

    // Rectangle 字段是 private final，用反射读取以保持金样与实现无关
    private static float width(Rectangle r) {
        return field(r, "width");
    }

    private static float height(Rectangle r) {
        return field(r, "height");
    }

    private static float startX(Rectangle r) {
        return field(r, "start_x");
    }

    private static float startY(Rectangle r) {
        return field(r, "start_y");
    }

    private static float field(Object o, String name) {
        try {
            var f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.getFloat(o);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    // ================= forts =================

    /**
     * 与 Setting.CompileForts 相同的解析规则（工具侧复刻），随后调用 Main.compileFort
     * 导出编译结果与 main_setup 的双侧生成参数；并用 Setting.CompileForts 交叉校验数量。
     */
    private static void forts(String outPath, String[] files) throws Exception {
        int idx = 0;
        for (String file : files) {
            String content = Files.readString(Path.of(file), StandardCharsets.UTF_8).trim();
            int parsed = 0;
            for (String part : content.split("/")) {
                if (part.isEmpty()) {
                    continue;
                }
                part = part.trim();
                if (part.isEmpty()) {
                    continue;
                }
                String name;
                String code;
                int amp = part.lastIndexOf('&');
                if (amp == -1) {
                    name = "";
                    code = part.replaceAll("[^a-zA-Z0-9]", "");
                    if (code.length() < 6) {
                        continue;
                    }
                } else {
                    name = part.substring(0, amp);
                    code = part.substring(amp + 1).replaceAll("[^a-zA-Z0-9]", "");
                    if (code.length() % 6 != 0) {
                        continue;
                    }
                }
                CompiledFort f = Main.compileFort(new Fort(name, code));
                parsed++;
                int coreType = iField(f, "coreType");
                int coreX = iField(f, "coreX");
                int coreY = iField(f, "coreY");
                int unitCount = iField(f, "unitCount");
                int[] type = iArray(f, "type");
                int[] xs = iArray(f, "x");
                int[] ys = iArray(f, "y");
                int[] rs = iArray(f, "r");
                SB.append("fort ").append(idx++).append(' ').append(code).append('\n');
                SB.append("core ").append(coreType).append(' ').append(coreX).append(' ')
                        .append(coreY).append('\n');
                SB.append("unitcount ").append(unitCount).append('\n');
                for (int k = 0; k < unitCount; k++) {
                    SB.append("unit ").append(k).append(' ').append(type[k]).append(' ')
                            .append(xs[k]).append(' ').append(ys[k]).append(' ').append(rs[k]).append('\n');
                    appendSpawn(0, k, xs[k], ys[k], rs[k]);
                    appendSpawn(1, k, xs[k], ys[k], rs[k]);
                }
            }
            int authoritative = org.example.Setting.CompileForts(file).size();
            if (authoritative != parsed) {
                throw new IllegalStateException(file + ": parsed " + parsed + " != Setting " + authoritative);
            }
        }
        Files.writeString(Path.of(outPath), SB.toString(), StandardCharsets.UTF_8);
        System.out.println("forts written to " + outPath + " (" + idx + " forts)");
    }

    /** 复刻 GameTask.main_setup 中单位生成参数的变换（含 float 运算顺序）。 */
    private static void appendSpawn(int side, int k, int x, int y, int r) {
        float ux = x;
        float uy = y;
        int rr = r;
        if (side == 1) {
            ux = (380 - ux) + 1490;
            rr = 180 - rr;
        } else {
            ux += 50;
        }
        uy += 132;
        rr = (rr % 360 + 360) % 360;
        SB.append("spawn").append(side).append(' ').append(k).append(' ')
                .append(b(ux)).append(' ').append(b(uy)).append(' ').append(rr).append('\n');
    }
}
