package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.HaneBullet;

public class HaneBall extends Ball {
    public HaneBall(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 50;
    }

    @Override
    public void stepEx() {
        if (this.cnt == this.speed) {
            this.cnt = 0;
            new HaneBullet(this.x, this.y, this.side, this.rot);
        }
    }
}
