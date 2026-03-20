package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.atk.PushBullet;

public class PushBall extends Ball {   //押玉
    private boolean attackQueued = false;

    public PushBall(float X, float Y, float R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        speed = 80;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed && this.jump_flg != 1) {
            this.attackQueued = true;
        }
        else if (this.cnt == this.speed + 6 && this.attackQueued) {
            this.attackQueued = false;
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 50;
            float spawnY = this.y + sin_rot * 50;
            new PushBullet(spawnX, spawnY, this.side, this.rot);
        }
    }
}
