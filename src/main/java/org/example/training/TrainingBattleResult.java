package org.example.training;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 面向 AI 训练的完整对局结果。
 */
public record TrainingBattleResult(
        int status,
        @JsonProperty("winner_hp") int winnerHp,
        int frames,
        @JsonProperty("time_ms") float timeMs,
        int seed,
        @JsonProperty("final_random_state") int finalRandomState,
        @JsonProperty("p1_hp") int p1Hp,
        @JsonProperty("p2_hp") int p2Hp,
        @JsonProperty("p1_name") String p1Name,
        @JsonProperty("p2_name") String p2Name,
        BattleTelemetry telemetry
) {
}
