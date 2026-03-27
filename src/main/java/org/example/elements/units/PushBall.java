package org.example.elements.units;

import org.example.GameTask;
import org.example.elements.Ball;
import org.example.elements.atk.PushBullet;

public class PushBall extends Ball {   //押玉

    public PushBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        speed = 80;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed + 6) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 50;
            float spawnY = this.y + sin_rot * 50;
            new PushBullet(game, spawnX, spawnY, this.side).setVecMult(cos_rot, sin_rot, 20).move();
        }
    }
}
