package org.example.elements.atk;

import java.util.ArrayList;
import java.util.List;
import org.example.Main;
import org.example.Shape;
import org.example.elements.Ball;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;

public class ConBullet extends Bullet {   //梱玉导弹
    private float speed = 1F;
    private int hp = 5;
    private int cnt = 0;
    private int nextTargetIndex = -1;

    public ConBullet(float X, float Y, int S, float rotation) {   //初始化
        super(X, Y, S);
        this.rot = rotation;
        this.r = 12.4F;
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

    private int pickTargetId() {   //挑选目标
        List<Shape> targets = Main.unit[1-side].getShapes();
        if (targets.isEmpty()) {
            return -1;
        }
        if (nextTargetIndex < 0 || nextTargetIndex >= targets.size()) {
            nextTargetIndex = targets.size() - 1;
        }
        nextTargetIndex--;
        return targets.get(nextTargetIndex).id;
    }

    private void updateVelocity() {   //更新速度向量
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * this.speed;
        this.ys = (float) Math.sin(rad) * this.speed;
    }
}
