package org.example.training;

/**
 * 对局遥测等级。事件模式逐帧检查关键状态；快照模式还会按固定间隔记录全局状态。
 */
public enum TelemetryMode {
    OFF,
    EVENTS,
    EVENTS_AND_SNAPSHOTS
}
