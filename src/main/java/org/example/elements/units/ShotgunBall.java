package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.atk.ShotgunBullet;

public class ShotgunBall extends Ball {   //散玉
    public ShotgunBall(float X, float Y, float R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        speed = 50;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 40;
            float spawnY = this.y + sin_rot * 40;
            for (int i = 0; i < 9; i++) {
                float atkRot = this.rot - 40 + 10 * i;
                new ShotgunBullet(spawnX, spawnY, this.side, atkRot);
            }
        }
    }
}
