package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.atk.BombBullet;

public class BombBall extends Ball {
    private boolean attackQueued = false;

    public BombBall(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 80;
    }

    @Override
    public void stepEx() {
        if (this.cnt == this.speed && this.jump_flg != 1) {
            this.attackQueued = true;
        }
        else if (this.cnt == this.speed + 1 && this.attackQueued) {
            this.attackQueued = false;
            this.cnt = 0;
            new BombBullet(this.x, this.y - 10, this.side).setVecMult(cos_rot, sin_rot, 10).setGravity(0.32F);
        }
    }
}
