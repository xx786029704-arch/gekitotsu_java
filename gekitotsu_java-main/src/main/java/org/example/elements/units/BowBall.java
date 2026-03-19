package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.Bullet;

public class BowBall extends Ball {     //弓玉
    public BowBall(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 30;
    }

    @Override
    public void stepEx(){
        if (this.cnt == this.speed + 6)
        {
            this.cnt = 0;
            new Bullet(this.x, this.y, this.side).setVecR(rot_radius, 15);
        }
    }
}
