package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.atk.HeriBullet;

public class HeriBall extends Ball {   //旋玉
    public HeriBall(float X, float Y, int R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        hp = 10;
        max_hp = 10;
        speed = 300;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed && this.jump_flg != 1) {
        }
        else if (this.cnt == this.speed + 4) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 25;
            float spawnY = this.y + sin_rot * 25;
            new HeriBullet(spawnX, spawnY, this.side, this.rot);
        }
    }
}
