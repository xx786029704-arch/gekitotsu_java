package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.BombBullet;

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
            float fireRad = (float) Math.toRadians(this.rot);
            new BombBullet(this.x, this.y - 10, this.side).setVecR(fireRad, 10);
        }
    }
}
