package org.example;

/**
 * 对局引擎的只读观测接口。
 *
 * <p>回调发生在初始化完成后、每一帧完整更新后以及结果生成后。实现类不得修改
 * {@link GameTask}，否则会破坏模拟结果的可复现性。</p>
 */
public interface BattleObserver {

    default void onBattleStarted(GameTask game) {
    }

    default void onFrameCompleted(GameTask game, int frame) {
    }

    default void onBattleFinished(GameTask game, Result result) {
    }
}
