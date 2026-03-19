package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.atk.HanabiBullet;

public class HanaBall extends Ball {
    public HanaBall(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 120;
    }

    @Override
    public void stepEx() {
        if (this.cnt == this.speed) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 45;
            float spawnY = this.y + sin_rot * 45;
            new HanabiBullet(spawnX, spawnY, this.side).setVecMult(cos_rot, sin_rot, 10).move();
        }
    }
}
