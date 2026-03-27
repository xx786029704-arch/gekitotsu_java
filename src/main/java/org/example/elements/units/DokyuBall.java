package org.example.elements.units;

import org.example.GameTask;
import org.example.elements.Ball;
import org.example.elements.Bullet;

public class DokyuBall extends Ball {   //弩玉
    public DokyuBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        speed = 30;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed + 6) {
            this.cnt = 0;
            new Bullet(game, x, y, side).setVecMult(cos_rot, sin_rot, 25).setGravity(0.32F);
        }
    }
}
