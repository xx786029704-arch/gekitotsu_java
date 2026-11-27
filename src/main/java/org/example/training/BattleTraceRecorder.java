package org.example.training;

import org.example.BattleObserver;
import org.example.GameTask;
import org.example.Result;
import org.example.Shape;
import org.example.elements.Ball;
import org.example.elements.Base;
import org.example.elements.Wall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 只读取帧末状态，不介入任何碰撞或单位逻辑。
 *
 * <p>跟踪对象为兵玉（unit 传感器）与要塞壁（wall 传感器，13 种子类；车板 Base 非 Wall，
 * 天然排除），两者统称 unit：伤害/治疗/阵亡/生成事件与聚合统计、快照存活数均包含要塞壁。
 */
final class BattleTraceRecorder implements BattleObserver {
    private final TelemetryMode mode;
    private final int snapshotInterval;
    private final List<BattleEvent> events = new ArrayList<>();
    private final List<BattleFrameSnapshot> snapshots = new ArrayList<>();
    private final Map<Integer, TrackedUnit> units = new LinkedHashMap<>();

    private int previousP1Hp;
    private int previousP2Hp;
    private int firstP1CoreDamageFrame = -1;
    private int firstP2CoreDamageFrame = -1;
    private int firstCollisionFrame = -1;
    private int collisionCount;
    private int p1CoreDamageTaken;
    private int p2CoreDamageTaken;
    private int p1CoreRepair;
    private int p2CoreRepair;
    private int p1UnitDamageTaken;
    private int p2UnitDamageTaken;
    private int p1UnitHealing;
    private int p2UnitHealing;
    private int p1UnitSpawns;
    private int p2UnitSpawns;
    private int p1UnitDeaths;
    private int p2UnitDeaths;
    private int p1Launches;
    private int p2Launches;

    BattleTraceRecorder(TelemetryMode mode, int snapshotInterval) {
        this.mode = mode;
        this.snapshotInterval = snapshotInterval;
    }

    @Override
    public void onBattleStarted(GameTask game) {
        previousP1Hp = game.hp[0];
        previousP2Hp = game.hp[1];
        discoverUnits(game, 0, false);
        if (mode == TelemetryMode.EVENTS_AND_SNAPSHOTS) {
            snapshots.add(snapshot(game, 0));
        }
    }

    @Override
    public void onFrameCompleted(GameTask game, int frame) {
        recordCoreHpChanges(game, frame);
        if (game.norikomi_flg) {
            if (firstCollisionFrame < 0) {
                firstCollisionFrame = frame;
            }
            collisionCount++;
            events.add(new BattleEvent(
                    frame,
                    BattleEventType.FORT_COLLISION,
                    -1,
                    -1,
                    -1,
                    -1,
                    (game.core_x[0] + game.core_x[1]) * 0.5F,
                    (game.core_y[0] + game.core_y[1]) * 0.5F,
                    0
            ));
        }
        discoverUnits(game, frame, true);
        recordUnitChanges(game, frame);
        if (mode == TelemetryMode.EVENTS_AND_SNAPSHOTS && frame % snapshotInterval == 0) {
            snapshots.add(snapshot(game, frame));
        }
    }

    @Override
    public void onBattleFinished(GameTask game, Result result) {
        if (mode == TelemetryMode.EVENTS_AND_SNAPSHOTS
                && (snapshots.isEmpty() || snapshots.getLast().frame() != result.framePassed)) {
            snapshots.add(snapshot(game, result.framePassed));
        }
    }

    BattleTelemetry toTelemetry() {
        BattleSummary summary = new BattleSummary(
                firstP1CoreDamageFrame,
                firstP2CoreDamageFrame,
                firstCollisionFrame,
                collisionCount,
                p1CoreDamageTaken,
                p2CoreDamageTaken,
                p1CoreRepair,
                p2CoreRepair,
                p1UnitDamageTaken,
                p2UnitDamageTaken,
                p1UnitHealing,
                p2UnitHealing,
                p1UnitSpawns,
                p2UnitSpawns,
                p1UnitDeaths,
                p2UnitDeaths,
                p1Launches,
                p2Launches
        );
        return new BattleTelemetry(summary, events, snapshots);
    }

    private void recordCoreHpChanges(GameTask game, int frame) {
        int[] previous = {previousP1Hp, previousP2Hp};
        for (int side = 0; side < 2; side++) {
            int delta = game.hp[side] - previous[side];
            if (delta < 0) {
                int amount = -delta;
                if (side == 0) {
                    p1CoreDamageTaken += amount;
                    if (firstP1CoreDamageFrame < 0) firstP1CoreDamageFrame = frame;
                } else {
                    p2CoreDamageTaken += amount;
                    if (firstP2CoreDamageFrame < 0) firstP2CoreDamageFrame = frame;
                }
                events.add(coreEvent(game, frame, BattleEventType.CORE_DAMAGE, side, amount));
            } else if (delta > 0) {
                if (side == 0) p1CoreRepair += delta;
                else p2CoreRepair += delta;
                events.add(coreEvent(game, frame, BattleEventType.CORE_REPAIR, side, delta));
            }
        }
        previousP1Hp = game.hp[0];
        previousP2Hp = game.hp[1];
    }

    private BattleEvent coreEvent(GameTask game, int frame, BattleEventType type, int side, int amount) {
        int id = game.cores[side] != null ? game.cores[side].id : -1;
        return new BattleEvent(
                frame,
                type,
                side,
                side,
                id,
                -1,
                game.core_x[side],
                game.core_y[side],
                amount
        );
    }

    private void discoverUnits(GameTask game, int frame, boolean recordSpawn) {
        for (int side = 0; side < 2; side++) {
            for (Shape shape : game.unit[side].getShapes()) {
                if (!(shape instanceof Ball ball) || units.containsKey(ball.id)) {
                    continue;
                }
                units.put(ball.id, new TrackedUnit(ball));
                if (recordSpawn) {
                    countSpawn(ball.side);
                    events.add(entityEvent(frame, BattleEventType.UNIT_SPAWN, ball, 0));
                }
            }
            for (Shape shape : game.wall[side].getShapes()) {
                if (!(shape instanceof Wall wall) || units.containsKey(wall.id)) {
                    continue;
                }
                units.put(wall.id, new TrackedUnit(wall));
                if (recordSpawn) {
                    countSpawn(wall.side);
                    events.add(entityEvent(frame, BattleEventType.UNIT_SPAWN, wall, 0));
                }
            }
        }
    }

    private void countSpawn(int side) {
        if (side == 0) p1UnitSpawns++;
        else p2UnitSpawns++;
    }

    private void recordUnitChanges(GameTask game, int frame) {
        for (TrackedUnit tracked : units.values()) {
            Shape shape = tracked.shape;
            boolean alive = game.elements.get(shape.id) == shape;
            if (alive) {
                recordUnitHpChange(tracked, frame);
                if (shape instanceof Ball ball) {
                    if (tracked.jumpFlag != 1 && ball.jump_flg == 1) {
                        if (ball.side == 0) p1Launches++;
                        else p2Launches++;
                        events.add(entityEvent(frame, BattleEventType.UNIT_LAUNCH, ball, 0));
                    } else if (tracked.jumpFlag == 1 && ball.jump_flg != 1) {
                        events.add(entityEvent(frame, BattleEventType.UNIT_LAND, ball, 0));
                    }
                }
                tracked.update();
            } else if (tracked.alive) {
                // 单位可能在本帧受到致命伤后立即从 elements 中移除，仍需记录最后一次 HP 变化。
                recordUnitHpChange(tracked, frame);
                tracked.update();
                tracked.alive = false;
                if (sideOf(shape) == 0) p1UnitDeaths++;
                else p2UnitDeaths++;
                events.add(entityEvent(frame, BattleEventType.UNIT_DEATH, shape, 0));
            }
        }
    }

    private void recordUnitHpChange(TrackedUnit tracked, int frame) {
        Shape shape = tracked.shape;
        int hpDelta = hpOf(shape) - tracked.hp;
        if (hpDelta < 0) {
            int amount = -hpDelta;
            if (sideOf(shape) == 0) p1UnitDamageTaken += amount;
            else p2UnitDamageTaken += amount;
            events.add(entityEvent(frame, BattleEventType.UNIT_DAMAGE, shape, amount));
        } else if (hpDelta > 0) {
            if (sideOf(shape) == 0) p1UnitHealing += hpDelta;
            else p2UnitHealing += hpDelta;
            events.add(entityEvent(frame, BattleEventType.UNIT_HEAL, shape, hpDelta));
        }
    }

    private static int hpOf(Shape shape) {
        return shape instanceof Ball ball ? ball.hp : ((Wall) shape).hp;
    }

    private static int sideOf(Shape shape) {
        return shape instanceof Ball ball ? ball.side : ((Wall) shape).side;
    }

    private static BattleEvent entityEvent(
            int frame,
            BattleEventType eventType,
            Shape shape,
            int amount
    ) {
        if (shape instanceof Ball ball) {
            return new BattleEvent(
                    frame,
                    eventType,
                    ball.side,
                    ball.on_side,
                    ball.id,
                    ball.type,
                    ball.x,
                    ball.y,
                    amount
            );
        }
        Wall wall = (Wall) shape;
        return new BattleEvent(
                frame,
                eventType,
                wall.side,
                wall.side,
                wall.id,
                wall.type,
                wall.x,
                wall.y,
                amount
        );
    }

    private BattleFrameSnapshot snapshot(GameTask game, int frame) {
        int p1Alive = 0;
        int p2Alive = 0;
        for (TrackedUnit tracked : units.values()) {
            if (tracked.alive) {
                if (sideOf(tracked.shape) == 0) p1Alive++;
                else p2Alive++;
            }
        }
        Base p1Base = game.bases[0];
        Base p2Base = game.bases[1];
        return new BattleFrameSnapshot(
                frame,
                game.hp[0],
                game.hp[1],
                p1Alive,
                p2Alive,
                game.atk[0].getShapes().size(),
                game.atk[1].getShapes().size(),
                p1Base != null,
                p2Base != null,
                p1Base != null ? p1Base.x : 0,
                p2Base != null ? p2Base.x : 0,
                p1Base != null ? p1Base.xs : 0,
                p2Base != null ? p2Base.xs : 0,
                game.core_x[0],
                game.core_y[0],
                game.core_x[1],
                game.core_y[1]
        );
    }

    private static final class TrackedUnit {
        private final Shape shape;
        private int hp;
        private int jumpFlag;
        private boolean alive = true;

        private TrackedUnit(Shape shape) {
            this.shape = shape;
            update();
        }

        private void update() {
            hp = hpOf(shape);
            if (shape instanceof Ball ball) {
                jumpFlag = ball.jump_flg;
            }
        }
    }
}
