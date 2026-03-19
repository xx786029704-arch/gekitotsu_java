package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.atk.KakuBullet;

public class KakuBall extends Ball {
    public KakuBall(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 350;
    }

    @Override
    public void stepEx() {
        if (this.cnt == this.speed + 4) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 45;
            float spawnY = this.y + sin_rot * 45;
            new KakuBullet(spawnX, spawnY, this.side, this.cos_rot, this.sin_rot);
        }
    }
}
