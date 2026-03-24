package org.example.elements.atk;

import java.util.List;
import org.example.Main;
import org.example.Shape;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;

public class ConBullet extends Bullet {   //梱玉导弹
    private float speed = 1F;
    private int hp = 5;
    private int cnt = 0;
    private int nextStartTargetIndex = -1;
    private final int rot;
    private final float cos_rot;
    private final float sin_rot;

    public ConBullet(float X, float Y, int S, int rotation, float _cos_rot, float _sin_rot) {   //初始化
        super(X, Y, S);
        this.rot = rotation;
        this.r = 12.4F;
        cos_rot = _cos_rot;
        sin_rot = _sin_rot;
        updateVelocity();
    }

    @Override
    public void step() {   //每帧逻辑
        cnt++;
        if (this.y < -1200 || this.x > 2560 || this.x < -640) {
            kill();
            return;
        }
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            this.hp--;
        }
        if (cnt == 150 || cnt == 152 || cnt == 154 || cnt == 156) {
            spawnMissiles();
            if (cnt == 156) {
                this.hp = 0;
            }
        }
        if (this.hp <= 0 || this.y > 570) {
            new HitsBomb(this.x, this.y, this.side);
            kill();
            return;
        }
        if (this.speed < 5F) {
            this.speed += 0.1F;
        }
        updateVelocity();
        move();
    }

    private void spawnMissiles() {   //生成子导弹
        new ConMissileBullet(this.x, this.y, this.side, this.rot - 90, pickTargetId());
        new ConMissileBullet(this.x, this.y, this.side, this.rot + 90, pickTargetId());
    }

    private int pickTargetId() {   //挑选目标（我从简化的逻辑改成了和原版一样的逻辑，就没报错了）
        List<Shape> targets = Main.unit[1 - side].getShapes();
        if (targets.isEmpty()) {
            return -1;
        }
        if (nextStartTargetIndex < 0 || nextStartTargetIndex >= targets.size()) {
            nextStartTargetIndex = targets.size() - 1;
        }
        int targetId = -1;
        int targetIndex = nextStartTargetIndex;
        while (targetIndex >= 0) {
            Shape wrk = targets.get(targetIndex);
            if (Main.elements.containsKey(wrk.id)) {
                targetId = wrk.id;
                nextStartTargetIndex = targetIndex - 1;
                break;
            }
            targetIndex--;
        }
        return targetId;
    }

    private void updateVelocity() {   //更新速度向量
        this.xs = cos_rot * this.speed;
        this.ys = sin_rot * this.speed;
    }
}
