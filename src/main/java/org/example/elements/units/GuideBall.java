package org.example.elements.units;

import org.example.GameTask;
import org.example.elements.Ball;
import org.example.elements.atk.MissileBullet;

public class GuideBall extends Ball {   //导玉
    public GuideBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        speed = 180;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 45;
            float spawnY = this.y + sin_rot * 45;
            new MissileBullet(game, spawnX, spawnY, this.side, this.rot);
        }
    }
}
