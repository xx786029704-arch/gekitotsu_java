package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.atk.KanBullet;

public class KanBall extends Ball {   //贯玉
    public KanBall(float X, float Y, float R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        speed = 170;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed + 7) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 50F;
            float spawnY = this.y + sin_rot * 50F;
            new KanBullet(spawnX, spawnY, this.side, this.rot);
        }
    }
}
