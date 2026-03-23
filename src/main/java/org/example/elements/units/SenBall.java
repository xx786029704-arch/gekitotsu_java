package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.atk.SenBullet;

public class SenBall extends Ball {   //战玉
    public SenBall(float X, float Y, int R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        hp = 10;
        max_hp = 10;
        speed = 300;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed && this.jump_flg != 1) {
        }
        else if (this.cnt == this.speed + 3) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 35;
            float spawnY = this.y + sin_rot * 35;
            new SenBullet(spawnX, spawnY, this.side, this.rot);
        }
    }
}
