package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.atk.ConBullet;

public class ConBall extends Ball {   //梱玉
    public ConBall(float X, float Y, int R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        speed = 200;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 15;
            float spawnY = this.y + sin_rot * 15;
            new ConBullet(spawnX, spawnY, this.side, this.rot, cos_rot, sin_rot);
        }
    }
}
