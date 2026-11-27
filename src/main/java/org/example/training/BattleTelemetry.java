package org.example.training;

import java.util.List;
import java.util.Objects;

/**
 * 一局对战的事件、定期快照与聚合统计。
 */
public record BattleTelemetry(
        BattleSummary summary,
        List<BattleEvent> events,
        List<BattleFrameSnapshot> snapshots
) {
    public BattleTelemetry {
        summary = Objects.requireNonNull(summary, "summary");
        events = List.copyOf(events);
        snapshots = List.copyOf(snapshots);
    }
}
