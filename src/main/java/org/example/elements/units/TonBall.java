package org.example.elements.units;

import org.example.Main;
import org.example.elements.Ball;
import org.example.elements.atk.TonBullet;

public class TonBall extends Ball {

    public TonBall(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 70;
    }

    @Override
    public void stepEx() {
        if (this.cnt == this.speed + 3) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 38;
            float spawnY = this.y + sin_rot * 38;
            new TonBullet(spawnX, spawnY, this.side).setGravity(0).setVecMult(cos_rot, sin_rot, 10).setGravity(0.32F);
        }
    }
}
