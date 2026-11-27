package org.example.training;

import org.example.CompiledFort;
import org.example.Fort;
import org.example.GameTask;
import org.example.Main;
import org.example.Result;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 只读遥测金样导出（L2+）：直接复用生产 {@link BattleTraceRecorder}，不修改引擎行为。
 *
 * <p>用法：
 * <pre>
 *   java ... org.example.training.TelemetryGoldenDump &lt;cases.txt&gt; &lt;out.bin&gt;
 * </pre>
 *
 * <p>cases 每行 {@code code1 code2 seed maxFrames}（# 开头为注释，seed 为 {@code -} 表示默认种子）。
 * 每个 case 依次导出三组配置：{@code (EVENTS,0)}、{@code (EVENTS_AND_SNAPSHOTS,1)}、
 * {@code (EVENTS_AND_SNAPSHOTS,7)}，顺序即二进制落盘顺序。
 *
 * <p>二进制（大端）：
 * <pre>
 *   u32 magic 'GKTT' = 0x474B5454, u32 version = 1, u32 runCount
 *   每条 run：
 *     i32 maxFrames, i32 seed, u8 hasSeed, u8 mode(1=EVENTS,2=FULL), i32 snapshotInterval,
 *     i32 status, i32 framePassed,
 *     summary: 18 * i32（字段顺序同 {@link BattleSummary} record）,
 *     u32 eventCount, 每条：i32 frame, i32 typeOrdinal, i32 side, i32 relatedSide,
 *                          i32 entityId, i32 unitType, u32 xBits, u32 yBits, i32 amount,
 *     u32 snapshotCount, 每条：i32 frame, i32 p1Hp, i32 p2Hp, i32 p1AliveUnits, i32 p2AliveUnits,
 *                          i32 p1Projectiles, i32 p2Projectiles, u8 p1BaseAlive, u8 p2BaseAlive,
 *                          u32 p1BaseX, u32 p2BaseX, u32 p1BaseSpeed, u32 p2BaseSpeed,
 *                          u32 p1CoreX, u32 p1CoreY, u32 p2CoreX, u32 p2CoreY
 * </pre>
 */
public final class TelemetryGoldenDump {

    private TelemetryGoldenDump() {
    }

    private record Config(TelemetryMode mode, int interval) {
    }

    private static final Config[] CONFIGS = {
            new Config(TelemetryMode.EVENTS, 0),
            new Config(TelemetryMode.EVENTS_AND_SNAPSHOTS, 1),
            new Config(TelemetryMode.EVENTS_AND_SNAPSHOTS, 7),
    };

    public static void main(String[] args) throws Exception {
        List<String[]> cases = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of(args[0]), StandardCharsets.UTF_8)) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("#")) {
                continue;
            }
            cases.add(t.split("\\s+"));
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream(1 << 22);
        DataOutputStream out = new DataOutputStream(bos);
        out.writeInt(0x474B5454);
        out.writeInt(1);
        out.writeInt(cases.size() * CONFIGS.length);

        for (String[] p : cases) {
            CompiledFort c1 = Main.compileFort(new Fort("", p[0]));
            CompiledFort c2 = Main.compileFort(new Fort("", p[1]));
            Integer seed = "-".equals(p[2]) ? null : Integer.valueOf(p[2]);
            int maxFrames = Integer.parseInt(p[3]);
            for (Config config : CONFIGS) {
                GameTask game = new GameTask();
                BattleTraceRecorder recorder = new BattleTraceRecorder(config.mode(), config.interval());
                Result result = game.run_single(c1, c2, maxFrames, seed, recorder);
                writeRun(out, maxFrames, seed, config, result, recorder.toTelemetry());
            }
        }

        Files.write(Path.of(args[1]), bos.toByteArray());
        System.out.println("telemetry written: " + (cases.size() * CONFIGS.length) + " runs -> " + args[1]);
    }

    private static void writeRun(
            DataOutputStream out,
            int maxFrames,
            Integer seed,
            Config config,
            Result result,
            BattleTelemetry telemetry
    ) throws Exception {
        out.writeInt(maxFrames);
        out.writeInt(seed == null ? 0 : seed);
        out.writeByte(seed == null ? 0 : 1);
        out.writeByte(config.mode() == TelemetryMode.EVENTS ? 1 : 2);
        out.writeInt(config.interval());
        out.writeInt(result.status);
        out.writeInt(result.framePassed);

        BattleSummary s = telemetry.summary();
        int[] summary = {
                s.firstP1CoreDamageFrame(),
                s.firstP2CoreDamageFrame(),
                s.firstCollisionFrame(),
                s.collisionCount(),
                s.p1CoreDamageTaken(),
                s.p2CoreDamageTaken(),
                s.p1CoreRepair(),
                s.p2CoreRepair(),
                s.p1UnitDamageTaken(),
                s.p2UnitDamageTaken(),
                s.p1UnitHealing(),
                s.p2UnitHealing(),
                s.p1UnitSpawns(),
                s.p2UnitSpawns(),
                s.p1UnitDeaths(),
                s.p2UnitDeaths(),
                s.p1Launches(),
                s.p2Launches(),
        };
        for (int value : summary) {
            out.writeInt(value);
        }

        out.writeInt(telemetry.events().size());
        for (BattleEvent e : telemetry.events()) {
            out.writeInt(e.frame());
            out.writeInt(e.type().ordinal());
            out.writeInt(e.side());
            out.writeInt(e.relatedSide());
            out.writeInt(e.entityId());
            out.writeInt(e.unitType());
            out.writeInt(Float.floatToRawIntBits(e.x()));
            out.writeInt(Float.floatToRawIntBits(e.y()));
            out.writeInt(e.amount());
        }

        out.writeInt(telemetry.snapshots().size());
        for (BattleFrameSnapshot snap : telemetry.snapshots()) {
            out.writeInt(snap.frame());
            out.writeInt(snap.p1Hp());
            out.writeInt(snap.p2Hp());
            out.writeInt(snap.p1AliveUnits());
            out.writeInt(snap.p2AliveUnits());
            out.writeInt(snap.p1Projectiles());
            out.writeInt(snap.p2Projectiles());
            out.writeByte(snap.p1BaseAlive() ? 1 : 0);
            out.writeByte(snap.p2BaseAlive() ? 1 : 0);
            out.writeInt(Float.floatToRawIntBits(snap.p1BaseX()));
            out.writeInt(Float.floatToRawIntBits(snap.p2BaseX()));
            out.writeInt(Float.floatToRawIntBits(snap.p1BaseSpeed()));
            out.writeInt(Float.floatToRawIntBits(snap.p2BaseSpeed()));
            out.writeInt(Float.floatToRawIntBits(snap.p1CoreX()));
            out.writeInt(Float.floatToRawIntBits(snap.p1CoreY()));
            out.writeInt(Float.floatToRawIntBits(snap.p2CoreX()));
            out.writeInt(Float.floatToRawIntBits(snap.p2CoreY()));
        }
    }
}
