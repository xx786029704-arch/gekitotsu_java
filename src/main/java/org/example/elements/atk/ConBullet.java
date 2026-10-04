package org.example.elements.atk;

import java.util.List;

import org.example.GameTask;
import org.example.Shape;
import org.example.Utils;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;

public class ConBullet extends Bullet {   //梱玉导弹
    private float speed = 1F;
    private int hp = 5;
    private int cnt = 0;
    private int nextTargetIndex = -1;
    private final int rot;
    private float cos_rot;
    private float sin_rot;

    public ConBullet(GameTask GAME, float X, float Y, int S, int rotation, float _cos_rot, float _sin_rot) {   //初始化
        super(GAME, X, Y, S);
        rot = rotation;
        r = 12.4F;
        cos_rot = _cos_rot;
        sin_rot = _sin_rot;
        updateVelocity();
    }

    @Override
    public void step() {   //每帧逻辑
        cnt++;
        if (y < -1200 || x > 2560 || x < -640) {
            kill();
            return;
        }
        if (game.team[1 - side].hitTestPoint(x, y) || gei_flg == 2) {
            hp--;
        }
        if (cnt == 150 || cnt == 152 || cnt == 154 || cnt == 156) {
            spawnMissiles();
            if (cnt == 156) {
                hp = 0;
            }
        }
        if (hp <= 0 || y > 570) {
            new HitsBomb(game, x, y, side);
            kill();
            return;
        }
        if (speed < 5F) {
            speed += 0.1F;
        }
        updateVelocity();
        move();
    }

    private void spawnMissiles() {   //生成子导弹
        new ConMissileBullet(game, x, y, side, rot - 90, pickTargetId());
        new ConMissileBullet(game, x, y, side, rot + 90, pickTargetId());
    }

    private int pickTargetId() {   //挑选目标
        List<Shape> targets = game.unit[1-side].getShapes();
        if (targets.isEmpty()) {
            return -1;
        }
        if (nextTargetIndex < 0 || nextTargetIndex >= targets.size()) {
            nextTargetIndex = targets.size() - 1;
        }
        return targets.get(nextTargetIndex--).id;
    }

    private void updateVelocity() {   //更新速度向量
        xs = cos_rot * speed;
        ys = sin_rot * speed;
    }

    @Override
    public void reflect(int from_rot){
        betray();
        cos_rot = Utils.cos(from_rot);
        sin_rot = Utils.sin(from_rot);
        updateVelocity();
    }
}
