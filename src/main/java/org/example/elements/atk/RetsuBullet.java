package org.example.elements.atk;

import org.example.Main;
import org.example.elements.Bullet;

public class RetsuBullet extends Bullet {

    public RetsuBullet(float X, float Y, int S) {
        super(X, Y, S);
    }

    @Override
    public void step() {
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.ys > 7 || this.gei_flg == 2) {
            float splitRot = (float) Math.toDegrees(Math.atan2(this.ys, this.xs));
            for (int i = 0; i < 7; i++) {
                float atkRot = splitRot - 30 + 10 * i;
                float rad = (float) Math.toRadians(atkRot);
                new Bullet(this.x, this.y, this.side).setVecR(rad, 20).move();
            }
            kill();
            return;
        }
        if (this.y > 570) {
            kill();
            return;
        }
        move();
    }
}
