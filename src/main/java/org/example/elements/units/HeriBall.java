package org.example.elements.units;

import org.example.GameTask;
import org.example.elements.Ball;
import org.example.elements.atk.HeriBullet;

public class HeriBall extends Ball {   //旋玉
    public HeriBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        speed = 300;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed + 4) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 25;
            float spawnY = this.y + sin_rot * 25;
            new HeriBullet(game, spawnX, spawnY, this.side, this.rot);
        }
    }
}
