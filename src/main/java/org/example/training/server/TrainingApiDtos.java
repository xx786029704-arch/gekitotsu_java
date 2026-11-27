package org.example.training.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.Main;
import org.example.training.BattleOptions;
import org.example.training.TelemetryMode;
import org.example.training.TrainingBattleResult;

import java.util.List;

public final class TrainingApiDtos {
    private TrainingApiDtos() {
    }

    public record FortDto(String name, String code) {
        String normalizedName() {
            return name == null ? "" : name;
        }
    }

    public record BattleRequest(
            FortDto fort1,
            FortDto fort2,
            @JsonProperty("max_frames") Integer maxFrames,
            Integer seed,
            TelemetryMode telemetry,
            @JsonProperty("snapshot_interval") Integer snapshotInterval
    ) {
        BattleOptions toOptions() {
            TelemetryMode actualMode = telemetry == null ? TelemetryMode.EVENTS : telemetry;
            int interval = actualMode == TelemetryMode.EVENTS_AND_SNAPSHOTS
                    ? (snapshotInterval == null ? 30 : snapshotInterval)
                    : 0;
            return new BattleOptions(
                    maxFrames == null ? Main.MAX_FRAME_LIMIT : maxFrames,
                    seed == null ? 0 : seed,
                    actualMode,
                    interval
            );
        }
    }

    public record BatchRequest(List<BattleRequest> battles) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BattleOutcome(
            TrainingBattleResult result,
            String error
    ) {
        static BattleOutcome success(TrainingBattleResult result) {
            return new BattleOutcome(result, null);
        }

        static BattleOutcome failure(String error) {
            return new BattleOutcome(null, error);
        }
    }

    public record BatchResponse(
            List<BattleOutcome> results,
            @JsonProperty("total_time_ms") double totalTimeMs
    ) {
    }

    public record SeedRequest(
            @JsonProperty("master_seed") long masterSeed,
            int count
    ) {
    }

    public record SeedResponse(
            @JsonProperty("master_seed") long masterSeed,
            int[] seeds
    ) {
    }

    public record ErrorResponse(String error, String detail) {
    }
}
