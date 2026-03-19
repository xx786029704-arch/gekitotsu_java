package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.TonBullet;

public class TonBall extends Ball {
    private boolean attackQueued = false;

    public TonBall(float X, float Y, float R, int S, int TYPE) {
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
            float fireRad = (float) Math.toRadians(this.rot);
            float spawnX = this.x + (float) Math.cos(fireRad) * 38;
            float spawnY = this.y + (float) Math.sin(fireRad) * 38;
            new TonBullet(spawnX, spawnY, this.side, this.rot);
        }
    }
}
