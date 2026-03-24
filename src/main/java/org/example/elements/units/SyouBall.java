package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.atk.SyouBullet;

public class SyouBall extends Ball {   //障玉
    public SyouBall(float X, float Y, int R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        hp = 10;
        max_hp = 10;
        speed = 160;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed + 7) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 30;
            float spawnY = this.y + sin_rot * 30;
            new SyouBullet(spawnX, spawnY, this.side, cos_rot, sin_rot);
        }
    }
}
