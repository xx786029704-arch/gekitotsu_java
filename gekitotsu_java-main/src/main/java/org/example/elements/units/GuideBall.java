package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.MissileBullet;

public class GuideBall extends Ball {
    public GuideBall(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 180;
    }

    @Override
    public void stepEx() {
        if (this.cnt == this.speed) {
            this.cnt = 0;
            float fireRad = (float) Math.toRadians(this.rot);
            float spawnX = this.x + (float) Math.cos(fireRad) * 45;
            float spawnY = this.y + (float) Math.sin(fireRad) * 45;
            new MissileBullet(spawnX, spawnY, this.side, this.rot);
        }
    }
}
