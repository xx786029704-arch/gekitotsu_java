package org.example.GUI;

import java.util.*;

/**
 * 突击检测器：输入阵容，输出其中的突击组件列表。
 *
 * <p>算法流程：</p>
 * <ol>
 *   <li>找到阵容中所有突击壁（Far=56, Near=55）</li>
 *   <li>对每个非墙壁单位，检查其中心是否在任一突击壁的 HitsJump 判定范围内</li>
 *   <li>解决归属冲突：Far 优先于 Near，同类型按规则分配</li>
 *   <li>合并所有 Near 突击组</li>
 * </ol>
 *
 * <p>HitsJump 判定形状（与 Wall 相同）：</p>
 * <ul>
 *   <li>中心位于 (wall.x, wall.y - 35)</li>
 *   <li>x 方向偏移 +0.3</li>
 *   <li>AABB [-16.85, 17.5] × [-17.5, 17.5]，四角圆角 r²=16</li>
 * </ul>
 */
public final class AssaultDetector {

    private AssaultDetector() {}

    // Near 和 Far 在 Unit.infos 中的 id
    private static final int NEAR_ID = 55;
    private static final int FAR_ID = 56;

    /**
     * 检测阵容中的突击组件，合并所有 Near 组后返回。
     *
     * @param units 阵容单位列表
     * @return 突击组列表（Near 已合并为一个），无突击壁时为空列表
     */
    public static List<AssaultGroup> detect(List<Unit> units) {
        if (units == null || units.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 收集所有突击壁的索引和类型
        List<Integer> wallIndices = new ArrayList<>();
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (u.id == NEAR_ID || u.id == FAR_ID) {
                wallIndices.add(i);
            }
        }
        if (wallIndices.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 为每个突击壁创建 AssaultGroup
        List<AssaultGroup> groups = new ArrayList<>();
        for (int wi : wallIndices) {
            Unit w = units.get(wi);
            groups.add(new AssaultGroup(w.id == FAR_ID, w.x, w.y, wi));
        }

        // 3. 对每个非墙壁兵玉，检测归属
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (u.isWallLike()) continue; // 墙壁和核心不受突击影响

            // 先检查 Far
            int farGroupIdx = findFarGroup(u, i, units, groups);
            if (farGroupIdx >= 0) {
                addUnitToGroup(u, i, groups.get(farGroupIdx));
                continue;
            }

            // 再检查 Near
            int nearGroupIdx = findNearGroup(u, i, units, groups);
            if (nearGroupIdx >= 0) {
                addUnitToGroup(u, i, groups.get(nearGroupIdx));
            }
        }

        // 4. 移除空组，合并所有 Near 组
        return mergeAndFilter(groups);
    }

    // ============ 归属判定 ============

    /**
     * 找到兵玉归属的 Near 突击壁索引（代码顺序最靠前的那个）。
     * 若兵玉在多个 Near 壁范围内，归于 wallIndex 最小的。
     * 返回 -1 表示不在任何 Near 壁范围内。
     */
    private static int findNearGroup(Unit unit, int unitIdx, List<Unit> units, List<AssaultGroup> groups) {
        int best = -1;
        int bestWallIdx = Integer.MAX_VALUE;
        for (int g = 0; g < groups.size(); g++) {
            AssaultGroup grp = groups.get(g);
            if (grp.isFar) continue; // 只看 Near
            if (isInJumpRange(unit, units.get(grp.wallIndex))) {
                if (grp.wallIndex < bestWallIdx) {
                    bestWallIdx = grp.wallIndex;
                    best = g;
                }
            }
        }
        return best;
    }

    /**
     * 找到兵玉归属的 Far 突击壁索引（x + 顺序分最大的那个）。
     * 分数 = wall.x + (unitIdx > wallIndex ? 1 : 0)，越大越优先。
     * 返回 -1 表示不在任何 Far 壁范围内。
     */
    private static int findFarGroup(Unit unit, int unitIdx, List<Unit> units, List<AssaultGroup> groups) {
        int best = -1;
        int bestScore = Integer.MIN_VALUE;
        for (int g = 0; g < groups.size(); g++) {
            AssaultGroup grp = groups.get(g);
            if (!grp.isFar) continue; // 只看 Far
            if (isInJumpRange(unit, units.get(grp.wallIndex))) {
                int score = grp.wallX + (unitIdx > grp.wallIndex ? 1 : 0);
                if (score > bestScore) {
                    bestScore = score;
                    best = g;
                }
            }
        }
        return best;
    }

    // ============ 判定范围 ============

    /**
     * 判断兵玉中心是否在突击壁的 HitsJump 判定范围内。
     *
     * <p>HitsJump 位于 (wall.x, wall.y - 35)，形状与 Wall.hitTestPoint 一致，
     * 但有 x+0.3 偏移。兵玉中心 (unit.x, unit.y) 为检测点。</p>
     */
    static boolean isInJumpRange(Unit unit, Unit wall) {
        // dx = ball.x - wall.x + 0.3  (HitsJump 的 x 偏移)
        float dx = (float) unit.x - (float) wall.x + 0.3f;
        // dy = ball.y - (wall.y - 35) = ball.y - wall.y + 35
        float dy = (float) unit.y - (float) wall.y + 35f;

        // 完全复刻 Wall.hitTestPoint / HitsJump.hitTestPoint 的形状检测
        if (dx < -16.85F || dx > 17.5F || dy < -17.5F || dy > 17.5F) {
            return false;
        }
        if (dx > -12.85F) {
            if (dx > 13.5F) {
                dx -= 13.5F;
            } else {
                return true;
            }
        } else {
            dx += 12.85F;
        }
        if (dy > -13.5F) {
            if (dy > 13.5F) {
                dy -= 13.5F;
            } else {
                return true;
            }
        } else {
            dy += 13.5F;
        }
        return dx * dx + dy * dy <= 16F;
    }

    // ============ 辅助方法 ============

    private static void addUnitToGroup(Unit unit, int unitIdx, AssaultGroup group) {
        if (unitIdx < group.wallIndex) {
            group.unitsBefore.add(unit);
        } else {
            group.unitsAfter.add(unit);
        }
    }

    /** 移除空组，并将所有 Near 组合并为一个。 */
    private static List<AssaultGroup> mergeAndFilter(List<AssaultGroup> groups) {
        AssaultGroup mergedNear = null;
        List<AssaultGroup> result = new ArrayList<>();

        for (AssaultGroup g : groups) {
            if (g.isEmpty()) continue;
            if (!g.isFar) {
                if (mergedNear == null) {
                    mergedNear = g;
                    result.add(g);
                } else {
                    mergedNear.mergeNear(g);
                }
            } else {
                result.add(g);
            }
        }
        return result;
    }

    // ============ 最速行动检测 ============

    /**
     * 检测所有突击组中的最速行动优化情况。
     *
     * <p>对每组突击中的每种兵玉，调用 {@link Unit#getQuickestXList(int, boolean)}
     * 获取最优 x 坐标集。若最优集非空且该种类中无任何单位放置在最优 x 上，
     * 则生成一条优化建议。</p>
     *
     * @param groups 已检测的突击组列表
     * @return 优化建议字符串列表，无建议时为空
     */
    public static List<String> checkQuickestX(List<AssaultGroup> groups) {
        List<String> suggestions = new ArrayList<>();
        if (groups == null || groups.isEmpty()) return suggestions;

        for (AssaultGroup g : groups) {
            if (!g.isFar) continue;
            // 收集该组中所有单位种类及其出现位置
            Set<Integer> seenIds = new HashSet<>();

            // 收集 unitsBefore 中的种类
            Set<Integer> beforeIds = new HashSet<>();
            for (Unit u : g.unitsBefore) {
                beforeIds.add(u.id);
                seenIds.add(u.id);
            }

            // 收集 unitsAfter 中的种类
            Set<Integer> afterIds = new HashSet<>();
            for (Unit u : g.unitsAfter) {
                afterIds.add(u.id);
                seenIds.add(u.id);
            }

            for (int id : seenIds) {
                // 获取该种类在 before/after 两个方向上的最优 x 集
                List<Integer> optimalBefore = beforeIds.contains(id)
                        ? Unit.getQuickestXList(id, g.wallX, true) : new ArrayList<>();
                List<Integer> optimalAfter = afterIds.contains(id)
                        ? Unit.getQuickestXList(id, g.wallX, false) : new ArrayList<>();

                // 如果该种类在两个方向上都没有最优解，跳过
                if (optimalBefore.isEmpty() && optimalAfter.isEmpty()) continue;

                // 检查是否已有任何单位采用最优 x
                boolean hasOptimal = false;
                for (Unit u : g.unitsBefore) {
                    if (u.id == id && optimalBefore.contains(u.x)) {
                        hasOptimal = true;
                        break;
                    }
                }
                if (!hasOptimal) {
                    for (Unit u : g.unitsAfter) {
                        if (u.id == id && optimalAfter.contains(u.x)) {
                            hasOptimal = true;
                            break;
                        }
                    }
                }

                if (!hasOptimal) {
                    StringBuilder sb = new StringBuilder();
                    String unitName = Unit.infos[id].name();
                    String type = g.isFar ? "远突击" : "近突击";
                    sb.append(unitName).append(" 在(").append(g.wallX).append(",").append(g.wallY)
                            .append(")处的").append(type).append("中没有采用落地最速，可能需要修改，推荐的x坐标为");

                    boolean first = true;
                    if (!optimalBefore.isEmpty()) {
                        sb.append(new TreeSet<>(optimalBefore)).append("（兵玉代码在前）");
                        first = false;
                    }
                    if (!optimalAfter.isEmpty()) {
                        if (!first) sb.append("，");
                        sb.append(new TreeSet<>(optimalAfter)).append("（兵玉代码在后）");
                    }
                    suggestions.add(sb.toString());
                }
            }
        }
        return suggestions;
    }
}
