package org.example.training;

/**
 * 第一版训练接口提供的关键事件。
 *
 * <p>HP 变化由帧末状态差分得到，因此同一帧内完全抵消的伤害与治疗不会出现。</p>
 */
public enum BattleEventType {
    CORE_DAMAGE,
    CORE_REPAIR,
    FORT_COLLISION,
    UNIT_DAMAGE,
    UNIT_HEAL,
    UNIT_SPAWN,
    UNIT_DEATH,
    UNIT_LAUNCH,
    UNIT_LAND
}
