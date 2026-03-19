package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.ShotgunBullet;

public class ShotgunBall extends Ball {
    public ShotgunBall(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 50;
    }

    @Override
    public void stepEx() {
        if (this.cnt == this.speed) {
            this.cnt = 0;
            float fireRad = (float) Math.toRadians(this.rot);
            float spawnX = this.x + (float) Math.cos(fireRad) * 40;
            float spawnY = this.y + (float) Math.sin(fireRad) * 40;
            for (int i = 0; i < 9; i++) {
                float atkRot = this.rot - 40 + 10 * i;
                new ShotgunBullet(spawnX, spawnY, this.side, atkRot);
            }
        }
    }
}
