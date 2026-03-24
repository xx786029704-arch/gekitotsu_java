package org.example.elements.units;

import org.example.Utils;
import org.example.elements.Ball;
import org.example.elements.atk.ShotgunBullet;

public class ShotgunBall extends Ball {   //散玉
    private Utils seeder;

    public ShotgunBall(float X, float Y, int R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        seeder = new Utils();
        speed = 50;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 40;
            float spawnY = this.y + sin_rot * 40;
            for (int i = 0; i < 9; i++) {
                int atkRot = this.rot - 40 + 10 * i;
                new ShotgunBullet(spawnX, spawnY, this.side).setVecR(atkRot, 27F + Utils.random(this.seeder) * 7F);
            }
        }
    }

    public void setSeed(int seed) {
        seeder.setSeed(seed);
    }
}
