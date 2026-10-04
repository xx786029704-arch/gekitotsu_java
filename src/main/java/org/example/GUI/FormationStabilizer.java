package org.example.GUI;

import java.util.ArrayList;
import java.util.List;

/**
 * 阵容稳定化器：模拟游戏第一帧碰撞检测，将因卡墙而被挤压的兵玉坐标还原到真实位置。
 *
 * <p>核心原理：游戏中 Ball.land() 会在每帧检查兵玉底部传感器 (x, y+16) 是否与
 * {@code wall[side]} 中的任何形状重叠，若重叠则以 1px 步长上移直至脱离。
 * 本类复刻此逻辑，对阵容中的所有非墙壁单位进行稳定化处理。</p>
 *
 * <p>用途：为 AnalysisPanel 等分析器提供真实坐标，避免因卡墙操作导致的误报（如
 * 两个兵玉代码坐标相近但因墙壁挤压实际位置已大幅偏离）。</p>
 */
public final class FormationStabilizer {

    private FormationStabilizer() {}

    // ============ 坐标变换常量（side 0）============
    // 兵玉/墙壁：gameX = unitX + 66, gameY = unitY + 152
    private static final float UNIT_GAME_OFFSET_X = 66f;
    private static final float UNIT_GAME_OFFSET_Y = 152f;
    // 核心：gameX = coreX + 102, gameY = coreY + 190
    private static final float CORE_GAME_OFFSET_X = 102f;
    private static final float CORE_GAME_OFFSET_Y = 190f;
    // 车板（side 0）
    private static final float BASE_GAME_X = 240f;
    private static final float BASE_GAME_Y = 532f;

    /**
     * 对阵容中所有非墙壁单位执行一帧碰撞稳定化，返回调整后的单位列表。
     * 墙壁和核心的坐标保持不变。
     *
     * @param units 原始阵容单位列表（第一个单位必须是核心）
     * @return 稳定化后的单位列表，顺序与原列表一致
     */
    public static List<Unit> stabilize(List<Unit> units) {
        if (units == null || units.size() < 2) {
            return units != null ? new ArrayList<>(units) : new ArrayList<>();
        }

        // 1. 找到核心并收集所有墙壁/核心的游戏坐标形状
        Unit coreUnit = null;
        List<CollisionShape> wallShapes = new ArrayList<>();

        for (Unit u : units) {
            if (u.isCore() && coreUnit == null) {
                coreUnit = u;
            }
            if (u.isWallLike()) {
                wallShapes.add(createShape(u, u == coreUnit));
            }
        }

        // 2. 车板也加入碰撞（虽通常不会与兵玉重叠，但包含它以完全还原游戏行为）
        wallShapes.add(new BaseShape(BASE_GAME_X, BASE_GAME_Y));

        // 3. 对每个非墙壁单位执行稳定化
        List<Unit> result = new ArrayList<>();
        for (Unit u : units) {
            if (!u.isWallLike()) {
                float gameX = u.x + UNIT_GAME_OFFSET_X;
                float gameY = u.y + UNIT_GAME_OFFSET_Y;
                float stableGameY = stabilizeY(gameX, gameY, wallShapes);
                int newY = Math.round(stableGameY - UNIT_GAME_OFFSET_Y);
                Unit stabilized = new Unit(u.id, u.x, newY, u.r);
                stabilized.hp = u.hp;
                result.add(stabilized);
            } else {
                result.add(new Unit(u.id, u.x, u.y, u.r)); // 墙壁坐标不变
            }
        }
        return result;
    }

    /**
     * 对单个兵玉执行稳定化：从初始位置开始，反复检查底部传感器是否与墙壁重叠，
     * 若重叠则上移 1px，直到脱离所有墙壁。
     *
     * @param gameX 兵玉的游戏绝对 X 坐标
     * @param gameY 兵玉的游戏绝对 Y 坐标
     * @param wallShapes 所有墙壁碰撞形状
     * @return 稳定化后的游戏绝对 Y 坐标（已量子化到 0.05）
     */
    private static float stabilizeY(float gameX, float gameY, List<CollisionShape> wallShapes) {
        // 复刻 land() 逻辑：ys += 1; drop_y = y + ys;
        float dropY = gameY + 1f;
        // 地面判定（drop_y >= 566 → 地面状态，跳过墙壁碰撞）
        if (dropY >= 566f) {
            return 566f; // ySync 后 566.0
        }
        // 传感器在兵玉中心下方 16px（与 Ball.land() 一致）
        // 上移直到传感器脱离所有墙壁形状
        while (hitTestAny(wallShapes, gameX, dropY + 15f)) {
            dropY -= 1f;
        }
        // ySync 量子化
        return (int) (20f * dropY) * 0.05f;
    }

    private static boolean hitTestAny(List<CollisionShape> shapes, float x, float y) {
        for (CollisionShape s : shapes) {
            if (s.hitTest(x, y)) {
                return true;
            }
        }
        return false;
    }

    // ============ 碰撞形状 ============

    private interface CollisionShape {
        boolean hitTest(float px, float py);
    }

    /** 根据单位类型创建对应的碰撞形状。 */
    private static CollisionShape createShape(Unit u, boolean isCore) {
        if (isCore) {
            return new CoreShape(u.x + CORE_GAME_OFFSET_X, u.y + CORE_GAME_OFFSET_Y);
        }
        // 标准墙壁（包括木/石/纸/铁/加速器/旋转壁/狙击壁/电梯壁等）
        return new WallShape(u.x + UNIT_GAME_OFFSET_X, u.y + UNIT_GAME_OFFSET_Y);
    }

    // ---- 标准墙壁形状 ----
    // 完全复刻 Wall.hitTestPoint 的逻辑
    private record WallShape(float wx, float wy) implements CollisionShape {
        @Override
        public boolean hitTest(float px, float py) {
            float dx = px - wx;
            float dy = py - wy;
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
    }

    // ---- 核心形状 ----
    // 完全复刻 Core.hitTestPoint 的逻辑
    private record CoreShape(float cx, float cy) implements CollisionShape {
        @Override
        public boolean hitTest(float px, float py) {
            float dx = px - cx;
            float dy = py - cy;
            if (dx < -53.5F || dx > 53.5F || dy < -53.5F || dy > 53.5F) {
                return false;
            }
            dx = Math.abs(dx);
            dy = Math.abs(dy);
            if (dy - dx <= 9.221316F && dy - dx >= -9.221316F && dx < 40) return true;
            if (dx * dx + dy * dy <= 1122.25F) return true;
            dx -= 40;
            dy -= 40;
            return dx * dx + dy * dy <= 182.25F;
        }
    }

    // ---- 车板形状 ----
    // 完全复刻 Base.hitTestPoint 的逻辑
    private record BaseShape(float bx, float by) implements CollisionShape {
        @Override
        public boolean hitTest(float px, float py) {
            float dx = px - bx;
            float dy = py - by;
            if (dx < -191.5F || dx > 191.5F || dy < -15.5F || dy > 51.5F) {
                return false;
            }
            if (dy > -4F) {
                if (dy > 17F) {
                    if (dy > 27.5F) {
                        dx = Math.abs(dx) - 108.7F;
                        dy -= 20;
                        return dx * dx + dy * dy <= 992.25F;
                    }
                    dy -= 17F;
                    dy *= 1.095238095238F;
                } else {
                    return true;
                }
            } else {
                dy += 4F;
            }
            if (dx > -180F) {
                if (dx > 180F) {
                    dx -= 180F;
                } else {
                    return true;
                }
            } else {
                dx += 180F;
            }
            return dx * dx + dy * dy <= 132.25F;
        }
    }
}
