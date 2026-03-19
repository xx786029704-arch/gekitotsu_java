package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.KakuBullet;

public class KakuBall extends Ball {
    private boolean attackQueued = false;

    public KakuBall(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 350;
    }

    @Override
    public void stepEx() {
        if (this.cnt == this.speed && this.jump_flg != 1) {
            this.attackQueued = true;
        }
        else if (this.cnt == this.speed + 4 && this.attackQueued) {
            this.attackQueued = false;
            this.cnt = 0;
            float fireRad = (float) Math.toRadians(this.rot);
            float spawnX = this.x + (float) Math.cos(fireRad) * 45;
            float spawnY = this.y + (float) Math.sin(fireRad) * 45;
            new KakuBullet(spawnX, spawnY, this.side, this.rot);
        }
    }
}
