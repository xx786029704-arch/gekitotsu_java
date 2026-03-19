package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.CannonBullet;

public class CannonBall extends Ball {
    public CannonBall(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 120;
    }

    @Override
    public void stepEx() {
        if (this.cnt == this.speed) {
            this.cnt = 0;
            float fireRad = (float) Math.toRadians(this.rot);
            float aRot = this.rot - 90;
            boolean flipped = !(this.rot < 90 + this.side || this.rot > 270 - this.side);
            if (flipped) {
                aRot -= 180;
            }
            float spawnRad = (float) Math.toRadians(aRot);
            float spawnX = this.x + (float) Math.cos(spawnRad) * 38;
            float spawnY = this.y + (float) Math.sin(spawnRad) * 38;
            new CannonBullet(spawnX, spawnY, this.side).setVecR(fireRad, 10);
        }
    }
}
