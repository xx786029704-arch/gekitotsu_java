package org.example.training;

/**
 * 一条结构化战斗事件。
 *
 * @param side        事件主体一方，0=1P，1=2P，-1=全局事件
 * @param relatedSide 单位当前承载/落地一方；不适用时为 -1
 * @param entityId    引擎元素 ID；不适用时为 -1
 * @param unitType    单位类型；不适用时为 -1
 * @param amount      HP 变化量等非负数值；不适用时为 0
 */
public record BattleEvent(
        int frame,
        BattleEventType type,
        int side,
        int relatedSide,
        int entityId,
        int unitType,
        float x,
        float y,
        int amount
) {
}
