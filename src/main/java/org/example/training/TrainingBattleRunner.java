package org.example.training;

import org.example.CompiledFort;
import org.example.Fort;
import org.example.GameTask;
import org.example.Main;
import org.example.Result;

import java.util.Objects;

/**
 * Nymph_Fort 2.0 使用的 Java 对局入口。
 */
public final class TrainingBattleRunner {
    private TrainingBattleRunner() {
    }

    public static TrainingBattleResult run(Fort p1, Fort p2, BattleOptions options) {
        Objects.requireNonNull(p1, "p1");
        Objects.requireNonNull(p2, "p2");
        Objects.requireNonNull(options, "options");
        validateCode(p1.code(), "p1");
        validateCode(p2.code(), "p2");

        CompiledFort compiledP1 = Main.compileFort(new Fort(normalizeName(p1.name()), p1.code()));
        CompiledFort compiledP2 = Main.compileFort(new Fort(normalizeName(p2.name()), p2.code()));
        return run(compiledP1, normalizeName(p1.name()), compiledP2, normalizeName(p2.name()), options);
    }

    public static TrainingBattleResult run(
            CompiledFort p1,
            String p1Name,
            CompiledFort p2,
            String p2Name,
            BattleOptions options
    ) {
        Objects.requireNonNull(p1, "p1");
        Objects.requireNonNull(p2, "p2");
        Objects.requireNonNull(options, "options");

        BattleTraceRecorder recorder = options.telemetryMode() == TelemetryMode.OFF
                ? null
                : new BattleTraceRecorder(options.telemetryMode(), options.snapshotInterval());
        GameTask game = new GameTask();
        Result result = game.run_single(
                p1,
                p2,
                options.maxFrames(),
                options.seed(),
                recorder
        );
        return new TrainingBattleResult(
                result.status,
                result.winnerHp,
                result.framePassed,
                result.timeUsed,
                options.seed(),
                game.seeder.getSeed(),
                game.hp[0],
                game.hp[1],
                normalizeName(p1Name),
                normalizeName(p2Name),
                recorder != null ? recorder.toTelemetry() : null
        );
    }

    public static void validateCode(String code, String fieldName) {
        if (code == null) {
            throw new IllegalArgumentException(fieldName + ".code 不能为空");
        }
        if (code.length() < 6 || code.length() % 6 != 0) {
            throw new IllegalArgumentException(fieldName + ".code 长度必须为不小于 6 的 6 的倍数");
        }
        if (code.charAt(0) < '0' || code.charAt(0) > '2') {
            throw new IllegalArgumentException(fieldName + ".code 的核心类型必须是 0、1 或 2");
        }
        for (int index = 0; index < code.length(); index++) {
            if (Main.pskey.indexOf(code.charAt(index)) < 0) {
                throw new IllegalArgumentException(
                        fieldName + ".code 在位置 " + index + " 含有非法字符"
                );
            }
        }
    }

    private static String normalizeName(String name) {
        return name == null ? "" : name;
    }
}
