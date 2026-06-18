package org.example.GUI;

import java.util.ArrayList;
import java.util.List;

/**
 * 突击组：一个突击壁及其上方可触发突击的兵玉集合。
 *
 * <p>术语定义：</p>
 * <ul>
 *   <li><b>突击</b>：Ball 触碰 jump_u（近突击壁）或 jump_f（远突击壁）判定，
 *       进入 jump_flg==1 状态的过程。由于 jump 判定持续时间极短（1帧），
 *       一般直接认为 Far/Near 造成了突击。</li>
 *   <li><b>一组突击</b>：一个突击壁 + 其上方可触碰到 jump 判定的兵玉的总称。
 *       有且仅有一个突击壁。</li>
 * </ul>
 *
 * <p>代码顺序影响：兵玉在突击壁之前/之后的执行顺序差异，在硬核玩家对战中
 * 可能成为决胜关键，因此分开记录。</p>
 */
public class AssaultGroup {

    /** 突击壁类型：true=远突击壁(Far)，false=近突击壁(Near) */
    public boolean isFar;

    /** 突击壁在阵容中的坐标 */
    public int wallX, wallY;

    /** 突击壁在阵容单位列表中的索引 */
    public int wallIndex;

    /** 代码顺序在突击壁<b>之前</b>的兵玉列表 */
    public final List<Unit> unitsBefore = new ArrayList<>();

    /** 代码顺序在突击壁<b>之后</b>的兵玉列表 */
    public final List<Unit> unitsAfter = new ArrayList<>();

    public AssaultGroup(boolean isFar, int wallX, int wallY, int wallIndex) {
        this.isFar = isFar;
        this.wallX = wallX;
        this.wallY = wallY;
        this.wallIndex = wallIndex;
    }

    /** 突击壁上方的总兵玉数 */
    public int unitCount() {
        return unitsBefore.size() + unitsAfter.size();
    }

    /** 该突击组是否为空（无兵玉在判定范围内） */
    public boolean isEmpty() {
        return unitsBefore.isEmpty() && unitsAfter.isEmpty();
    }

    /** 将另一个 Near 突击组合并到当前实例（会清空对方）。 */
    public void mergeNear(AssaultGroup other) {
        if (this.isFar || other.isFar) {
            throw new IllegalArgumentException("只有近突击组之间可以合并");
        }
        this.unitsBefore.addAll(other.unitsBefore);
        this.unitsAfter.addAll(other.unitsAfter);
        other.unitsBefore.clear();
        other.unitsAfter.clear();
    }

    @Override
    public String toString() {
        String type = isFar ? "远突击" : "近突击";
        return type + "壁(" + wallX + "," + wallY + ") "
                + "前" + unitsBefore.size() + "个 后" + unitsAfter.size() + "个";
    }
}
