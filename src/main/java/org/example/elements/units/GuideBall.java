package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.atk.MissileBullet;

public class GuideBall extends Ball {   //导玉
    public GuideBall(float X, float Y, float R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        speed = 180;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 45;
            float spawnY = this.y + sin_rot * 45;
            new MissileBullet(spawnX, spawnY, this.side, this.rot);
        }
    }
}
