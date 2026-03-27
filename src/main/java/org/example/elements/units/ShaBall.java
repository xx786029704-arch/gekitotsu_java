package org.example.elements.units;

import org.example.GameTask;
import org.example.elements.Ball;
import org.example.elements.atk.ShaBullet;

public class ShaBall extends Ball {   //射玉
    public ShaBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        speed = 70;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 70;
            float spawnY = this.y + sin_rot * 70;
            new ShaBullet(game, spawnX, spawnY, this.side, cos_rot * 10F, sin_rot * 10F);
        }
    }
}
