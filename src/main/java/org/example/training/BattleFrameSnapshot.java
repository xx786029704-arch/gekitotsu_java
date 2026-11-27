package org.example.training;

/**
 * 固定间隔采样的全局对局状态。坐标均为引擎世界坐标。
 */
public record BattleFrameSnapshot(
        int frame,
        int p1Hp,
        int p2Hp,
        int p1AliveUnits,
        int p2AliveUnits,
        int p1Projectiles,
        int p2Projectiles,
        boolean p1BaseAlive,
        boolean p2BaseAlive,
        float p1BaseX,
        float p2BaseX,
        float p1BaseSpeed,
        float p2BaseSpeed,
        float p1CoreX,
        float p1CoreY,
        float p2CoreX,
        float p2CoreY
) {
}
