package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.NinBullet;

public class NinBall extends Ball {
    private boolean attackQueued = false;

    public NinBall(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 70;
    }

    @Override
    public void stepEx() {
        if (this.cnt == this.speed && this.jump_flg != 1) {
            this.attackQueued = true;
        }
        else if (this.cnt == this.speed + 3 && this.attackQueued) {
            this.attackQueued = false;
            this.cnt = 0;
            new NinBullet(this.x, this.y, this.side, this.rot);
        }
    }
}
