package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.Bullet;
import org.example.elements.atk.GunBullet;

public class GunBall extends Ball {     //铳玉
    public GunBall(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 60;
    }

    @Override
    public void stepEx(){
        if (this.cnt == this.speed || this.cnt == this.speed + 2 || this.cnt == this.speed + 4) {
            if (cnt == this.speed + 4) {
                this.cnt = 0;
            }
            new GunBullet(this.x, this.y, this.side).setVecMult(cos_rot, sin_rot, 20).move();
        }
    }
}
