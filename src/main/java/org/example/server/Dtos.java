package org.example.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.Result;

import java.util.List;

public class Dtos {

    public record FortDto(String name, String code) {}

    public record BattleRequest(
            FortDto fort1,
            FortDto fort2,
            @JsonProperty("max_frames") Integer maxFrames
    ) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record BattleResponse(
            int status,
            @JsonProperty("winner_hp") int winnerHp,
            long frames,
            @JsonProperty("time_ms") double timeMs,
            @JsonProperty("fort1_name") String fort1Name,
            @JsonProperty("fort2_name") String fort2Name,
            String error
    ) {
        public static BattleResponse success(Result r, String fort1Name, String fort2Name) {
            return new BattleResponse(r.status, r.winnerHp, r.framePassed, r.timeUsed, fort1Name, fort2Name, null);
        }

        public static BattleResponse error(String message) {
            return new BattleResponse(-2, 0, 0, 0, null, null, message);
        }
    }

    public record BatchRequest(List<BattleRequest> battles) {}

    public record BatchResponse(
            List<BattleResponse> results,
            @JsonProperty("total_time_ms") double totalTimeMs
    ) {}

    public record FortSlotRequest(List<FortDto> forts) {}

    public record FortSlotResponse(int count, List<String> names) {}

    public record RunRequest(@JsonProperty("max_frames") Integer maxFrames) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorResponse(String error, String detail) {}
}
