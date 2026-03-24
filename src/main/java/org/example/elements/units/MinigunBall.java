package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.Bullet;
import org.example.elements.atk.GunBullet;

public class MinigunBall extends Ball {   //机玉
    public MinigunBall(float X, float Y, int R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        speed = 200;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt >= this.speed && this.cnt % 4 == 0 && this.jump_flg != 1) {
            float vx = cos_rot * 22;
            float vy = sin_rot * 22;
            new GunBullet(this.x + vx * 3, this.y + vy * 3, this.side).setVec(vx, vy).setPower(22F);
        }
        if (this.cnt >= this.speed + 200) {
            this.cnt = 0;
        }
    }

    @Override
    public void hurt(boolean is_crash) {   //受击重置计时
        if (!is_crash) {
            this.cnt = 0;
        }
    }
}
