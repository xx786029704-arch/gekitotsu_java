package org.example.tools;

import org.example.BattleObserver;
import org.example.CompiledFort;
import org.example.Fort;
import org.example.GameTask;
import org.example.Main;
import org.example.Result;
import org.example.Shape;
import org.example.elements.Base;
import org.example.elements.Ball;
import org.example.elements.BossCore;
import org.example.elements.BossCore2;
import org.example.elements.Core;
import org.example.elements.Wall;
import org.example.elements.hit.BossLaser;
import org.example.elements.hit.BossLaser2;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;
import org.example.elements.hit.HitsBombMult;
import org.example.elements.hit.HitsBone;
import org.example.elements.hit.MagicLaser;
import org.example.elements.hit.StarLaser;
import org.example.elements.hit.StarPrepareLaser;
import org.example.elements.hit.HitsDrop;
import org.example.elements.hit.HitsDropFrames;
import org.example.elements.hit.HitsHeal;
import org.example.elements.hit.HitsJump;
import org.example.elements.hit.HitsKabe;
import org.example.elements.hit.HitsKen;
import org.example.elements.hit.HitsKnight;
import org.example.elements.hit.HitsNagi;
import org.example.elements.hit.HitsYari;
import org.example.elements.hit.KekkaiField;
import org.example.elements.atk.BombBullet;
import org.example.elements.atk.BossMissileBullet;
import org.example.elements.atk.DrillCreature;
import org.example.elements.atk.ShockCreature;
import org.example.elements.atk.SolidCreature;
import org.example.elements.atk.WeakCreature;
import org.example.elements.atk.CannonBullet;
import org.example.elements.atk.ConBullet;
import org.example.elements.atk.ConMissileBullet;
import org.example.elements.atk.GeiBullet;
import org.example.elements.atk.GekiBullet;
import org.example.elements.atk.HanabiBullet;
import org.example.elements.atk.HaneBullet;
import org.example.elements.atk.HeriBullet;
import org.example.elements.atk.HinokoBullet;
import org.example.elements.atk.HolyBullet;
import org.example.elements.atk.KakuBullet;
import org.example.elements.atk.KanBullet;
import org.example.elements.atk.MissileBullet;
import org.example.elements.atk.NinBullet;
import org.example.elements.atk.PushBullet;
import org.example.elements.atk.SearchBullet;
import org.example.elements.atk.SenBullet;
import org.example.elements.atk.SenTamaBullet;
import org.example.elements.atk.SyouBullet;
import org.example.elements.atk.TobiBombBullet;
import org.example.elements.atk.TobiBullet;
import org.example.elements.atk.TonBullet;
import org.example.elements.atk.TuiBullet;
import org.example.elements.atk.TuiMissileBullet;
import org.example.elements.atk.UkiBullet;
import org.example.elements.atk.GunBullet;
import org.example.elements.atk.RetsuBullet;
import org.example.elements.atk.ShaBullet;
import org.example.elements.atk.ShotgunBullet;
import org.example.elements.atk.SniperBullet;
import org.example.elements.units.BombBall;
import org.example.elements.units.BoxBall;
import org.example.elements.units.DarkBall;
import org.example.elements.units.HenBall;
import org.example.elements.units.MagicBall;
import org.example.elements.units.StarBall;
import org.example.elements.units.BoneBall;
import org.example.elements.units.ConBall;
import org.example.elements.units.GeiBall;
import org.example.elements.units.GekiBall;
import org.example.elements.units.GuideBall;
import org.example.elements.units.HanaBall;
import org.example.elements.units.HaneBall;
import org.example.elements.units.HeriBall;
import org.example.elements.units.HolyBall;
import org.example.elements.units.KakuBall;
import org.example.elements.units.KanBall;
import org.example.elements.units.NinBall;
import org.example.elements.units.PushBall;
import org.example.elements.units.SearchBall;
import org.example.elements.units.SenBall;
import org.example.elements.units.ShaBall;
import org.example.elements.units.SyouBall;
import org.example.elements.units.TobiBall;
import org.example.elements.units.TonBall;
import org.example.elements.units.TuiBall;
import org.example.elements.units.UkiBall;
import org.example.elements.units.BowBall;
import org.example.elements.units.CannonBall;
import org.example.elements.units.DokyuBall;
import org.example.elements.units.GunBall;
import org.example.elements.units.MinigunBall;
import org.example.elements.units.RetsuBall;
import org.example.elements.units.ShotgunBall;
import org.example.elements.units.SniperBall;
import org.example.elements.units.HanBall;
import org.example.elements.units.HealBall;
import org.example.elements.units.KabeBall;
import org.example.elements.units.KekkaiBall;
import org.example.elements.units.KnightBall;
import org.example.elements.units.NagiBall;
import org.example.elements.units.NieBall;
import org.example.elements.units.RepairBall;
import org.example.elements.units.SaiBall;
import org.example.elements.units.SwordBall;
import org.example.elements.units.TargetBall;
import org.example.elements.units.TateBall;
import org.example.elements.units.YariBall;
import org.example.elements.wall.Box;
import org.example.elements.wall.Elevator;
import org.example.elements.wall.Far;
import org.example.elements.wall.Iron;
import org.example.elements.wall.Jet;
import org.example.elements.wall.Narrow;
import org.example.elements.wall.Near;
import org.example.elements.wall.Paper;
import org.example.elements.wall.Snipe;
import org.example.elements.wall.Stone;
import org.example.elements.wall.Turbo;
import org.example.elements.wall.Wide;
import org.example.elements.wall.Wood;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 只读金样导出：对局级结果（L2）与逐帧状态（L3），供 Rust 引擎对拍。
 *
 * <p>用法：
 * <pre>
 *   java ... org.example.tools.GameDump battles <cases.txt> <out.txt>
 *   java ... org.example.tools.GameDump frames  <out.bin> &lt;code1&gt; &lt;code2&gt; &lt;seed|-&gt; &lt;maxFrames&gt;
 * </pre>
 *
 * <p>cases 每行：{@code code1 code2 seed maxFrames}（# 开头为注释，seed 为 {@code -} 表示默认种子）。
 * battles 输出每行：{@code b status winnerHp framePassed finalRandomState p1Hp p2Hp}。
 *
 * <p>frames 二进制（大端）：magic 'GKTF' u32, version u32, count u32；随后 count 条帧记录；
 * 末尾 trailer：finalRandomState i32, status i32, winnerHp i32, framePassed i32。
 */
public final class GameDump {

    private GameDump() {
    }

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "battles" -> battles(args[1], args[2]);
            case "frames" -> frames(args[1], args[2], args[3], args[4], Integer.parseInt(args[5]));
            default -> {
                System.err.println("unknown mode: " + args[0]);
                System.exit(2);
            }
        }
    }

    private static CompiledFort compile(String code) {
        return Main.compileFort(new Fort("", code));
    }

    private static Integer parseSeed(String s) {
        return "-".equals(s) ? null : Integer.valueOf(s);
    }

    private static void battles(String casesPath, String outPath) throws Exception {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String line : Files.readAllLines(Path.of(casesPath), StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            String[] p = t.split("\\s+");
            GameTask g = new GameTask();
            Result r = g.run_single(compile(p[0]), compile(p[1]), Integer.parseInt(p[3]), parseSeed(p[2]), null);
            sb.append("b ").append(r.status).append(' ').append(r.winnerHp).append(' ')
                    .append(r.framePassed).append(' ').append(g.seeder.getSeed()).append(' ')
                    .append(g.hp[0]).append(' ').append(g.hp[1]).append('\n');
            count++;
        }
        Files.writeString(Path.of(outPath), sb.toString(), StandardCharsets.UTF_8);
        System.out.println("battles written: " + count + " -> " + outPath);
    }

    private static final Field F_XX = field("xx");
    private static final Field F_YY = field("yy");
    private static final Field F_ITEM_KEYS = itemKeysField();

    private static Field itemKeysField() {
        try {
            Field f = org.example.IntShapeMap.class.getDeclaredField("itemKeys");
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static int[] itemKeys(GameTask game) {
        try {
            return (int[]) F_ITEM_KEYS.get(game.elements);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Field field(String name) {
        try {
            Field f = Base.class.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void frames(String outPath, String code1, String code2, String seedS, int maxFrames)
            throws Exception {
        List<byte[]> records = new ArrayList<>();
        BattleObserver observer = new BattleObserver() {
            @Override
            public void onFrameCompleted(GameTask game, int frame) {
                records.add(frameRecord(game, frame));
            }
        };
        GameTask g = new GameTask();
        Result r = g.run_single(compile(code1), compile(code2), maxFrames, parseSeed(seedS), observer);

        ByteArrayOutputStream bos = new ByteArrayOutputStream(1 << 20);
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(0x474B5446);   // magic 'GKTF'
        dos.writeInt(1);
        dos.writeInt(records.size());
        for (byte[] rec : records) {
            dos.write(rec);
        }
        dos.writeInt(g.seeder.getSeed());
        dos.writeInt(r.status);
        dos.writeInt(r.winnerHp);
        dos.writeInt(r.framePassed);
        Files.write(Path.of(outPath), bos.toByteArray());
        System.out.println("frames written: " + records.size() + " -> " + outPath);
    }

    private static byte[] frameRecord(GameTask game, int frame) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream(256);
            DataOutputStream d = new DataOutputStream(bos);
            d.writeInt(frame);
            d.writeInt(game.seeder.getSeed());
            d.writeInt(game.mid);
            d.writeInt(game.hp[0]);
            d.writeInt(game.hp[1]);
            d.writeInt(game.hp0_flg[0]);
            d.writeInt(game.hp0_flg[1]);
            d.writeByte(game.dokkan_flg[0] ? 1 : 0);
            d.writeByte(game.dokkan_flg[1] ? 1 : 0);
            d.writeByte(game.norikomi_flg ? 1 : 0);
            for (int i = 0; i <= 1; i++) {
                Base b = game.bases[i];
                if (b == null) {
                    for (int k = 0; k < 6; k++) {
                        d.writeFloat(Float.NaN);
                    }
                } else {
                    d.writeFloat(b.x);
                    d.writeFloat(b.y);
                    d.writeFloat(b.xs);
                    d.writeFloat(b.ys);
                    d.writeFloat(F_XX.getFloat(b));
                    d.writeFloat(F_YY.getFloat(b));
                }
            }
            for (int i = 0; i <= 1; i++) {
                Core c = game.cores[i];
                if (c == null) {
                    d.writeFloat(Float.NaN);
                    d.writeFloat(Float.NaN);
                } else {
                    d.writeFloat(c.x);
                    d.writeFloat(c.y);
                }
            }
            int alive = 0;
            Shape[] items = game.elements.items;
            int size = game.elements.size;
            int[] keys = itemKeys(game);
            for (int i = 0; i < size; i++) {
                if (items[i] != null) {
                    alive++;
                }
            }
            d.writeInt(alive);
            for (int i = 0; i < size; i++) {
                Shape s = items[i];
                if (s == null) {
                    continue;
                }
                // 使用 IntShapeMap 的真实 key（LaserBase.id 遮蔽了 Shape.id；数组下标会被 compact 改变）
                d.writeInt(keys[i]);
                int kind = kindOf(s);
                d.writeByte(kind);
                d.writeFloat(s.x);
                d.writeFloat(s.y);
                if (kind >= 7 && kind <= 19) {
                    d.writeInt(((Wall) s).hp);
                    d.writeInt(((Wall) s).breaking);
                }
                switch (kind) {
                    case 5, 6 -> {
                        d.writeFloat((Float) lfield(s, "rotDeg"));
                        d.writeFloat((Float) lfield(s, "xSpeed"));
                        d.writeFloat((Float) lfield(s, "ySpeed"));
                        d.writeInt((Integer) lfield(s, "steps"));
                        d.writeFloat((Float) lfield(s, "startX"));
                        d.writeFloat((Float) lfield(s, "startY"));
                    }
                    case 14 -> d.writeInt((Integer) lfield(s, "cnt"));
                    case 15, 16 -> {
                        d.writeInt((Integer) lfield(s, "cnt"));
                        d.writeByte(((Boolean) lfield(s, "mode")) ? 1 : 0);
                    }
                    case 18 -> d.writeInt((Integer) lfield(s, "ys"));
                    case 19 -> {
                        d.writeInt((Integer) lfield(s, "on_side"));
                        d.writeFloat((Float) lfield(s, "ys"));
                    }
                    case 21 -> d.writeInt((Integer) lfield(s, "frames"));
                    case 30, 32, 33, 36 -> {
                        d.writeInt((Integer) lfield(s, "frame"));
                        d.writeInt((Integer) lfield(s, "user"));
                        d.writeFloat((Float) lfield(s, "cos_rot"));
                        d.writeFloat((Float) lfield(s, "sin_rot"));
                        d.writeByte(((Boolean) lfield(s, "flipped")) ? 1 : 0);
                    }
                    case 31 -> {
                        d.writeInt((Integer) lfield(s, "frame"));
                        d.writeInt((Integer) lfield(s, "user"));
                        d.writeInt((Integer) lfield(s, "rot"));
                        java.util.List<Shape> shapes = ((org.example.CompositeShape) s).getShapes();
                        d.writeInt(shapes.size());
                        for (Shape sub : shapes) {
                            d.writeFloat(sub.x);
                            d.writeFloat(sub.y);
                            d.writeInt((Integer) lfield(sub, "flipRad"));
                        }
                    }
                    case 34 -> {
                        d.writeInt((Integer) lfield(s, "frame"));
                        d.writeInt((Integer) lfield(s, "user"));
                        d.writeFloat((Float) lfield(s, "cos_rot"));
                        d.writeFloat((Float) lfield(s, "sin_rot"));
                    }
                    case 35 -> d.writeFloat(((org.example.Round) s).r);
                    case 45 -> d.writeInt((Integer) lfield(s, "frame"));
                    case 46 -> {
                        d.writeInt((Integer) lfield(s, "frame"));
                        d.writeFloat((Float) lfield(s, "mult"));
                    }
                    case 47, 48 -> {
                        d.writeFloat((Float) lfield(s, "xs"));
                        d.writeFloat((Float) lfield(s, "ys"));
                    }
                    case 49, 50, 51, 52, 53 -> {
                        Bullet b = (Bullet) s;
                        d.writeInt(b.gei_flg);
                        d.writeFloat(b.xs);
                        d.writeFloat(b.ys);
                        d.writeFloat(b.gravity);
                        d.writeFloat(b.power);
                        d.writeInt(b.side);
                        if (kind == 51) {
                            d.writeInt((Integer) lfield(s, "hp"));
                        }
                        if (kind == 52) {
                            d.writeInt((Integer) lfield(s, "cnt"));
                        }
                    }
                    case 108, 110 -> {
                        d.writeFloat((Float) lfield(s, "rotDeg"));
                        d.writeFloat((Float) lfield(s, "xSpeed"));
                        d.writeFloat((Float) lfield(s, "ySpeed"));
                        d.writeInt((Integer) lfield(s, "steps"));
                        d.writeFloat((Float) lfield(s, "startX"));
                        d.writeFloat((Float) lfield(s, "startY"));
                        if (kind == 110) {
                            d.writeInt((Integer) lfield(s, "cnt"));
                        }
                    }
                    case 109 -> {
                        // StarPrepareLaser.rotDeg 是 int
                        d.writeInt((Integer) lfield(s, "rotDeg"));
                        d.writeFloat((Float) lfield(s, "xSpeed"));
                        d.writeFloat((Float) lfield(s, "ySpeed"));
                        d.writeFloat((Float) lfield(s, "startX"));
                        d.writeFloat((Float) lfield(s, "startY"));
                        d.writeInt((Integer) lfield(s, "steps"));
                        d.writeInt((Integer) lfield(s, "cnt"));
                        d.writeFloat((Float) lfield(s, "oldRotDeg"));
                        d.writeInt((Integer) lfield(s, "side"));
                    }
                    case 111 -> {
                        Bullet b = (Bullet) s;
                        d.writeInt(b.gei_flg);
                        d.writeFloat(b.xs);
                        d.writeFloat(b.ys);
                        d.writeFloat(b.gravity);
                        d.writeFloat(b.power);
                        d.writeInt(b.side);
                        d.writeInt((Integer) lfield(s, "cnt"));
                        d.writeFloat((Float) lfield(s, "targetRot"));
                        d.writeInt((Integer) lfield(s, "targetId"));
                        d.writeFloat((Float) lfield(s, "rot"));
                        d.writeFloat((Float) lfield(s, "targetRotDelta"));
                        d.writeFloat(((org.example.Round) s).r);
                    }
                    case 112 -> {
                        d.writeInt((Integer) lfield(s, "hp"));
                        d.writeFloat((Float) lfield(s, "xuv"));
                        d.writeFloat((Float) lfield(s, "yuv"));
                        d.writeFloat((Float) lfield(s, "speed"));
                    }
                    case 113 -> {
                        d.writeInt((Integer) lfield(s, "hp"));
                        d.writeFloat((Float) lfield(s, "xs"));
                        d.writeFloat((Float) lfield(s, "ys"));
                        d.writeFloat((Float) lfield(s, "rot"));
                    }
                    case 114 -> {
                        d.writeInt((Integer) lfield(s, "hp"));
                        d.writeFloat((Float) lfield(s, "xs"));
                        d.writeFloat((Float) lfield(s, "ys"));
                        d.writeFloat((Float) lfield(s, "rot"));
                        d.writeInt((Integer) lfield(s, "cnt"));
                        d.writeInt((Integer) lfield(s, "yrot"));
                        d.writeFloat((Float) lfield(s, "ycos"));
                        d.writeFloat((Float) lfield(s, "ysin"));
                    }
                    case 115 -> {
                        d.writeInt((Integer) lfield(s, "hp"));
                        d.writeFloat((Float) lfield(s, "xs"));
                        d.writeFloat((Float) lfield(s, "ys"));
                        d.writeFloat((Float) lfield(s, "rot"));
                        d.writeFloat((Float) lfield(s, "xs2"));
                    }
                    case 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81 -> {
                        Bullet b = (Bullet) s;
                        d.writeInt(b.gei_flg);
                        d.writeFloat(b.xs);
                        d.writeFloat(b.ys);
                        d.writeFloat(b.gravity);
                        d.writeFloat(b.power);
                        d.writeInt(b.side);
                        switch (kind) {
                            case 63 -> {
                                d.writeInt((Integer) lfield(s, "cnt"));
                                d.writeInt((Integer) lfield(s, "hp"));
                                d.writeFloat((Float) lfield(s, "speed"));
                                d.writeInt((Integer) lfield(s, "rot"));
                                d.writeFloat((Float) lfield(s, "cos_rot"));
                                d.writeFloat((Float) lfield(s, "sin_rot"));
                            }
                            case 64 -> {
                                d.writeInt((Integer) lfield(s, "cnt"));
                                d.writeInt((Integer) lfield(s, "targetId"));
                                d.writeFloat((Float) lfield(s, "speed"));
                                d.writeFloat((Float) lfield(s, "desiredRot"));
                                d.writeFloat((Float) lfield(s, "rot"));
                            }
                            case 65 -> {
                                d.writeFloat((Float) lfield(s, "speed"));
                                d.writeFloat((Float) lfield(s, "cos_rot"));
                                d.writeFloat((Float) lfield(s, "sin_rot"));
                            }
                            case 66, 69 -> d.writeInt((Integer) lfield(s, "cnt"));
                            case 67, 73 -> d.writeInt((Integer) lfield(s, "bounceCount"));
                            case 68 -> {
                                d.writeInt((Integer) lfield(s, "hp"));
                                d.writeInt((Integer) lfield(s, "cnt"));
                                d.writeInt((Integer) lfield(s, "yrot"));
                                d.writeFloat((Float) lfield(s, "speed"));
                                d.writeFloat((Float) lfield(s, "rot"));
                            }
                            case 70 -> {
                                d.writeInt((Integer) lfield(s, "hp"));
                                d.writeFloat((Float) lfield(s, "speed"));
                                d.writeFloat((Float) lfield(s, "cos_rot"));
                                d.writeFloat((Float) lfield(s, "sin_rot"));
                            }
                            case 72 -> {
                                d.writeInt((Integer) lfield(s, "cnt"));
                                d.writeFloat((Float) lfield(s, "speed"));
                                d.writeFloat((Float) lfield(s, "rot"));
                            }
                            case 75 -> {
                                d.writeInt((Integer) lfield(s, "hp"));
                                d.writeInt((Integer) lfield(s, "cnt"));
                                d.writeByte(((Boolean) lfield(s, "drop_flg")) ? 1 : 0);
                            }
                            case 76 -> d.writeInt((Integer) lfield(s, "hp"));
                            case 77 -> {
                                d.writeInt((Integer) lfield(s, "hp"));
                                d.writeInt((Integer) lfield(s, "cnt"));
                                d.writeFloat((Float) lfield(s, "speed"));
                                d.writeFloat((Float) lfield(s, "rot"));
                            }
                            case 79 -> {
                                d.writeInt((Integer) lfield(s, "hp"));
                                d.writeInt((Integer) lfield(s, "bounceCount"));
                            }
                            case 80 -> {
                                d.writeInt((Integer) lfield(s, "hp"));
                                d.writeInt((Integer) lfield(s, "cnt"));
                                d.writeByte(((Boolean) lfield(s, "drop_flg")) ? 1 : 0);
                            }
                            case 81 -> {
                                d.writeFloat((Float) lfield(s, "speed"));
                                d.writeFloat((Float) lfield(s, "cos_rot"));
                                d.writeFloat((Float) lfield(s, "sin_rot"));
                            }
                            default -> {
                            }
                        }
                    }
                    case 82 -> {
                        d.writeInt((Integer) lfield(s, "user_id"));
                        d.writeInt((Integer) lfield(s, "target_id"));
                    }
                    case 83 -> {
                        d.writeInt((Integer) lfield(s, "cnt"));
                        d.writeInt((Integer) lfield(s, "t_type"));
                        d.writeInt((Integer) lfield(s, "t_rot"));
                        d.writeInt((Integer) lfield(s, "t_side"));
                        d.writeInt((Integer) lfield(s, "t_on_side"));
                        d.writeInt((Integer) lfield(s, "t_jump_flg"));
                    }
                    case 84 -> {
                        d.writeInt((Integer) lfield(s, "hp"));
                        d.writeInt((Integer) lfield(s, "cnt"));
                        d.writeFloat((Float) lfield(s, "power"));
                        d.writeByte(((Boolean) lfield(s, "active")) ? 1 : 0);
                    }
                    case 85 -> {
                        d.writeFloat((Float) lfield(s, "dirX"));
                        d.writeFloat((Float) lfield(s, "dirY"));
                        d.writeFloat((Float) lfield(s, "xs"));
                        d.writeFloat((Float) lfield(s, "ys"));
                    }
                    case 86 -> {
                        d.writeInt((Integer) lfield(s, "hp"));
                        d.writeInt((Integer) lfield(s, "yrot"));
                        d.writeFloat((Float) lfield(s, "ysin"));
                        d.writeFloat((Float) lfield(s, "ycos"));
                    }
                    case 22, 23, 24, 25, 26, 27, 28, 37, 38, 39, 40, 41, 42, 43, 44, 54, 55, 56, 57, 58, 59, 60, 61, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 116, 117, 118, 119, 120 -> {
                        Ball b = (Ball) s;
                        d.writeInt(b.hp);
                        d.writeInt(b.max_hp);
                        d.writeInt(b.jump_flg);
                        d.writeInt(b.on_side);
                        d.writeInt(b.cnt);
                        d.writeInt(b.rot);
                        d.writeFloat(b.xs);
                        d.writeFloat(b.ys);
                        d.writeFloat(b.cos_rot);
                        d.writeFloat(b.sin_rot);
                        d.writeFloat(b.drop_y);
                        d.writeInt(b.speed);
                        switch (kind) {
                            case 24 -> d.writeInt(((NieBall) s).alarm);
                            case 26 -> {
                                d.writeInt(((TateBall) s).hurt_time);
                                d.writeByte(((Boolean) lfield(s, "flipped")) ? 1 : 0);
                            }
                            case 27 -> d.writeInt(((HanBall) s).t_id);
                            case 28 -> d.writeInt(((KekkaiBall) s).hurt_time);
                            case 41 -> {
                                d.writeByte(((KabeBall) s).shooting ? 1 : 0);
                                d.writeInt((Integer) lfield(s, "unit_x"));
                            }
                            case 44 -> {
                                d.writeInt(((KnightBall) s).hurt_time);
                                d.writeByte(((Boolean) lfield(s, "flipped")) ? 1 : 0);
                            }
                            default -> {
                            }
                        }
                    }
                    default -> {
                    }
                }
            }
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 沿类继承链找字段（LaserBase 的字段声明在父类） */
    private static Object lfield(Object o, String name) {
        Class<?> c = o.getClass();
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        throw new IllegalStateException("field not found: " + name);
    }

    private static int kindOf(Shape s) {
        if (s instanceof Base) {
            return 0;
        }
        if (s instanceof BossCore) {
            return 1;
        }
        if (s instanceof BossCore2) {
            return 2;
        }
        if (s instanceof Core) {
            return 3;
        }
        if (s instanceof KekkaiField) {
            return 4;
        }
        if (s instanceof BossLaser2) {
            return 6;
        }
        if (s instanceof BossLaser) {
            return 5;
        }
        if (s instanceof Wood) {
            return 7;
        }
        if (s instanceof Stone) {
            return 8;
        }
        if (s instanceof Paper) {
            return 9;
        }
        if (s instanceof Iron) {
            return 10;
        }
        if (s instanceof Jet) {
            return 11;
        }
        if (s instanceof Turbo) {
            return 12;
        }
        if (s instanceof Near) {
            return 13;
        }
        if (s instanceof Far) {
            return 14;
        }
        if (s instanceof Wide) {
            return 15;
        }
        if (s instanceof Narrow) {
            return 16;
        }
        if (s instanceof Snipe) {
            return 17;
        }
        if (s instanceof Elevator) {
            return 18;
        }
        if (s instanceof Box) {
            return 19;
        }
        if (s instanceof HitsJump) {
            return 20;
        }
        if (s instanceof HitsDropFrames) {
            return 21;
        }
        if (s instanceof HitsDrop) {
            return 29;
        }
        if (s instanceof HitsBombMult) {
            return 46;
        }
        if (s instanceof HitsBomb) {
            return 45;
        }
        if (s instanceof SniperBullet) {
            return 48;
        }
        if (s instanceof BossMissileBullet) {
            return 111;
        }
        if (s instanceof MagicLaser) {
            return 108;
        }
        if (s instanceof StarLaser) {
            return 110;
        }
        if (s instanceof StarPrepareLaser) {
            return 109;
        }
        if (s instanceof DrillCreature) {
            return 112;
        }
        if (s instanceof SolidCreature) {
            return 113;
        }
        if (s instanceof ShockCreature) {
            return 114;
        }
        if (s instanceof WeakCreature) {
            return 115;
        }
        if (s instanceof BombBullet) {
            return 62;
        }
        if (s instanceof ConMissileBullet) {
            return 64;
        }
        if (s instanceof ConBullet) {
            return 63;
        }
        if (s instanceof GekiBullet) {
            return 65;
        }
        if (s instanceof HanabiBullet) {
            return 66;
        }
        if (s instanceof HaneBullet) {
            return 67;
        }
        if (s instanceof HeriBullet) {
            return 68;
        }
        if (s instanceof HinokoBullet) {
            return 69;
        }
        if (s instanceof KakuBullet) {
            return 70;
        }
        if (s instanceof KanBullet) {
            return 71;
        }
        if (s instanceof MissileBullet) {
            return 72;
        }
        if (s instanceof NinBullet) {
            return 73;
        }
        if (s instanceof PushBullet) {
            return 74;
        }
        if (s instanceof SenTamaBullet) {
            return 76;
        }
        if (s instanceof SenBullet) {
            return 75;
        }
        if (s instanceof TobiBombBullet) {
            return 78;
        }
        if (s instanceof TobiBullet) {
            return 77;
        }
        if (s instanceof TonBullet) {
            return 79;
        }
        if (s instanceof TuiMissileBullet) {
            return 81;
        }
        if (s instanceof TuiBullet) {
            return 80;
        }
        if (s instanceof GeiBullet) {
            return 82;
        }
        if (s instanceof HolyBullet) {
            return 83;
        }
        if (s instanceof SyouBullet) {
            return 84;
        }
        if (s instanceof SearchBullet) {
            return 85;
        }
        if (s instanceof UkiBullet) {
            return 86;
        }
        if (s instanceof ShaBullet) {
            return 47;
        }
        if (s instanceof GunBullet) {
            return 50;
        }
        if (s instanceof CannonBullet) {
            return 51;
        }
        if (s instanceof ShotgunBullet) {
            return 52;
        }
        if (s instanceof RetsuBullet) {
            return 53;
        }
        if (s instanceof Bullet) {
            return 49;
        }
        if (s instanceof HitsKen) {
            return 30;
        }
        if (s instanceof HitsYari) {
            return 31;
        }
        if (s instanceof HitsNagi) {
            return 32;
        }
        if (s instanceof HitsBone) {
            return 33;
        }
        if (s instanceof HitsKabe) {
            return 34;
        }
        if (s instanceof HitsHeal) {
            return 35;
        }
        if (s instanceof HitsKnight) {
            return 36;
        }
        if (s instanceof MagicBall) {
            return 116;
        }
        if (s instanceof HenBall) {
            return 117;
        }
        if (s instanceof BoxBall) {
            return 118;
        }
        if (s instanceof DarkBall) {
            return 119;
        }
        if (s instanceof StarBall) {
            return 120;
        }
        if (s instanceof BombBall) {
            return 87;
        }
        if (s instanceof ConBall) {
            return 88;
        }
        if (s instanceof GeiBall) {
            return 89;
        }
        if (s instanceof GekiBall) {
            return 90;
        }
        if (s instanceof GuideBall) {
            return 91;
        }
        if (s instanceof HanaBall) {
            return 92;
        }
        if (s instanceof HaneBall) {
            return 93;
        }
        if (s instanceof HeriBall) {
            return 94;
        }
        if (s instanceof HolyBall) {
            return 95;
        }
        if (s instanceof KakuBall) {
            return 96;
        }
        if (s instanceof KanBall) {
            return 97;
        }
        if (s instanceof NinBall) {
            return 98;
        }
        if (s instanceof PushBall) {
            return 99;
        }
        if (s instanceof SearchBall) {
            return 100;
        }
        if (s instanceof SenBall) {
            return 101;
        }
        if (s instanceof ShaBall) {
            return 102;
        }
        if (s instanceof SyouBall) {
            return 103;
        }
        if (s instanceof TobiBall) {
            return 104;
        }
        if (s instanceof TonBall) {
            return 105;
        }
        if (s instanceof TuiBall) {
            return 106;
        }
        if (s instanceof UkiBall) {
            return 107;
        }
        if (s instanceof BowBall) {
            return 54;
        }
        if (s instanceof DokyuBall) {
            return 55;
        }
        if (s instanceof GunBall) {
            return 56;
        }
        if (s instanceof CannonBall) {
            return 57;
        }
        if (s instanceof ShotgunBall) {
            return 58;
        }
        if (s instanceof MinigunBall) {
            return 59;
        }
        if (s instanceof SniperBall) {
            return 60;
        }
        if (s instanceof RetsuBall) {
            return 61;
        }
        if (s instanceof SwordBall) {
            return 37;
        }
        if (s instanceof YariBall) {
            return 38;
        }
        if (s instanceof NagiBall) {
            return 39;
        }
        if (s instanceof BoneBall) {
            return 40;
        }
        if (s instanceof KabeBall) {
            return 41;
        }
        if (s instanceof HealBall) {
            return 42;
        }
        if (s instanceof RepairBall) {
            return 43;
        }
        if (s instanceof KnightBall) {
            return 44;
        }
        if (s instanceof TargetBall) {
            return 23;
        }
        if (s instanceof NieBall) {
            return 24;
        }
        if (s instanceof SaiBall) {
            return 25;
        }
        if (s instanceof TateBall) {
            return 26;
        }
        if (s instanceof HanBall) {
            return 27;
        }
        if (s instanceof KekkaiBall) {
            return 28;
        }
        if (s instanceof Ball) {
            return 22;
        }
        return 255;
    }
}
