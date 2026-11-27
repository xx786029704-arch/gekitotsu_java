package org.example.training;

/**
 * 适合直接作为行为特征输入的轻量统计。
 */
public record BattleSummary(
        int firstP1CoreDamageFrame,
        int firstP2CoreDamageFrame,
        int firstCollisionFrame,
        int collisionCount,
        int p1CoreDamageTaken,
        int p2CoreDamageTaken,
        int p1CoreRepair,
        int p2CoreRepair,
        int p1UnitDamageTaken,
        int p2UnitDamageTaken,
        int p1UnitHealing,
        int p2UnitHealing,
        int p1UnitSpawns,
        int p2UnitSpawns,
        int p1UnitDeaths,
        int p2UnitDeaths,
        int p1Launches,
        int p2Launches
) {
}
