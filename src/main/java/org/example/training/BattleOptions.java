package org.example.training;

import org.example.Main;
import org.example.Utils;

import java.util.Objects;

/**
 * 单局训练参数。
 */
public record BattleOptions(
        int maxFrames,
        int seed,
        TelemetryMode telemetryMode,
        int snapshotInterval
) {
    public BattleOptions {
        if (maxFrames < 0) {
            throw new IllegalArgumentException("maxFrames 不能为负数");
        }
        seed = Utils.normalizeSeed(seed);
        telemetryMode = Objects.requireNonNull(telemetryMode, "telemetryMode");
        if (telemetryMode == TelemetryMode.EVENTS_AND_SNAPSHOTS && snapshotInterval <= 0) {
            throw new IllegalArgumentException("启用快照时 snapshotInterval 必须大于 0");
        }
        if (telemetryMode != TelemetryMode.EVENTS_AND_SNAPSHOTS) {
            snapshotInterval = 0;
        }
    }

    public static BattleOptions events(int seed) {
        return new BattleOptions(Main.MAX_FRAME_LIMIT, seed, TelemetryMode.EVENTS, 0);
    }

    public static BattleOptions full(int seed, int snapshotInterval) {
        return new BattleOptions(
                Main.MAX_FRAME_LIMIT,
                seed,
                TelemetryMode.EVENTS_AND_SNAPSHOTS,
                snapshotInterval
        );
    }

    public static BattleOptions withoutTelemetry(int seed) {
        return new BattleOptions(Main.MAX_FRAME_LIMIT, seed, TelemetryMode.OFF, 0);
    }
}
