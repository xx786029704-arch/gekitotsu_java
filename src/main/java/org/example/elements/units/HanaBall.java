package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.atk.HanabiBullet;

public class HanaBall extends Ball {   //花玉
    public HanaBall(float X, float Y, int R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        speed = 120;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 45;
            float spawnY = this.y + sin_rot * 45;
            new HanabiBullet(spawnX, spawnY, this.side, rot).setVecMult(cos_rot, sin_rot, 10).move();
        }
    }
}
