package org.example.elements.atk;

import org.example.Game;
import org.example.Round;
import org.example.Utils;
import org.example.elements.Bullet;
import org.example.elements.Core;
import org.example.elements.hit.HitsBomb;

public class MissileBullet extends Bullet {   //导玉导弹
    private int cnt = 0;
    private float speed = 4F;
    private float rot;

    public MissileBullet(Game game, float X, float Y, int S, int rotation) {   //初始化
        super(game, X, Y, S);
        this.rot = rotation;
        this.gei_flg = 1;
        updateVelocity();
    }

    @Override
    public void step() {   //每帧逻辑
        cnt++;
        if (this.y < -1200 || this.x > 2560 || this.x < -640) {
            kill();
            return;
        }
        if (this.game.team[1 - this.side].hitTestPoint(this.x, this.y) || this.y > 570 || this.gei_flg == 2 || cnt > 200) {
            new HitsBomb(this.game, this.x, this.y, this.side);
            kill();
            return;
        }
        float targetRot = Math.round((float) Math.toDegrees(Math.atan2(this.game.core_y[1-side] - this.y, this.game.core_x[1-side] - this.x)));
        if (this.rot - 180 > targetRot) {
            targetRot += 360;
        }
        if (this.rot + 180 < targetRot) {
            targetRot -= 360;
        }
        if (this.speed < 15F) {
            this.speed += 0.3F;
        }
        if (this.speed > 14F) {
            this.rot = this.rot + (targetRot - this.rot) / 8F;
        }
        updateVelocity();
        move();
    }

    private void updateVelocity() {   //更新速度向量
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * this.speed;
        this.ys = (float) Math.sin(rad) * this.speed;
    }

    @Override
    public void reflect(int from_rot){
        betray();
        rot = from_rot;
        updateVelocity();
    }
}
