package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.atk.UkiBullet;

public class UkiBall extends Ball {   //浮玉
    public UkiBall(float X, float Y, float R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        speed = 150;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed + 8) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 50;
            float spawnY = this.y + sin_rot * 50;
            new UkiBullet(spawnX, spawnY, this.side, cos_rot * 3, sin_rot * 3);
        }
    }
}
