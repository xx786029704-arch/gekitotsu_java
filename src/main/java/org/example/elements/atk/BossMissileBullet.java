package org.example.elements.atk;

import org.example.GameTask;
import org.example.Shape;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;

public class BossMissileBullet extends Bullet {   //导玉导弹
    private int cnt = 0;
    private float targetRot;
    private final int targetId;
    private float rot;
    private float targetRotDelta;

    public BossMissileBullet(GameTask GAME, float X, float Y, int S, int target, float rng1, float rng2, float rng3, float rng4, float rng5) {   //初始化
        super(GAME, X, Y, S);
        this.r = 9.3F;
        this.targetRot = Math.round(rng1 * 360);
        this.rot = this.targetRot;
        this.targetId = target;
        this.xs = Math.round(rng2 * 16) - 8;
        this.ys = Math.round(rng3 * 10) - 16;
        this.gei_flg = 1;
        setGravity(.2f);
        this.targetRotDelta = Math.round(rng4 * 8) + 4;
        if (Math.round(rng5 * 2) != 0) {
            this.targetRotDelta = this.targetRotDelta * 4;
        } else {
            this.targetRotDelta = (-this.targetRotDelta) * 4;
        }
    }

    @Override
    public void step() {   //每帧逻辑
        cnt++;
        if (this.cnt < 60) {
            this.rot += this.targetRotDelta;
            this.rot = ((this.rot % 360) + 360) % 360;
            move();
            if (this.y < -1200 || this.x > 2560 || this.x < -640) {
                kill();
                return;
            }
            if (game.team[1 - this.side].hitTestPoint(this.x, this.y) || this.y > 570 || this.gei_flg == 2) {
                new HitsBomb(game, this.x, this.y, this.side);
                kill();
            }
        } else {
            setGravity(0.F);
            if (this.y < -1200 || this.x > 2560 || this.x < -640) {
                kill();
                return;
            }
            if (game.team[1 - this.side].hitTestPoint(this.x, this.y) || this.y > 570 || this.gei_flg == 2 || cnt > 260) {
                new HitsBomb(game, this.x, this.y, this.side);
                kill();
                return;
            }
            if (targetId != -1) {
                Shape target = game.elements.get(targetId);
                if (target != null) {
                    targetRot = Math.round((float) (Math.atan2(target.y - this.y, target.x - this.x) * 180 / Math.PI));
                }
            } else {
                targetRot = Math.round((float) (Math.atan2(game.core_y[1 - this.side] - this.y, game.core_x[1 - this.side] - this.x) * 180 / Math.PI));
            }
            if (this.rot - 180 > this.targetRot) {
                this.targetRot = this.targetRot + 360;
            }
            if (this.rot + 180 < this.targetRot) {
                this.targetRot = this.targetRot - 360;
            }
            this.rot = this.rot * .875F + this.targetRot * .125F;
            updateVelocity();
            move();
        }
    }

    private void updateVelocity() {   //更新速度向量
        float rad = (float) Math.toRadians(this.targetRot);
        this.xs = (float) Math.cos(rad) * 20F;
        this.ys = (float) Math.sin(rad) * 20F;
    }
}