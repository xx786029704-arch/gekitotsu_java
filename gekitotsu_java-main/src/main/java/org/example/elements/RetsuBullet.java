package org.example.elements;

import org.example.Main;

public class RetsuBullet extends Bullet {
    private final float power;

    public RetsuBullet(float X, float Y, int S, float rotation, float power) {
        super(X, Y, S);
        this.power = power;
        this.rot = rotation;
        this.gei_flg = 1;
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * this.power;
        this.ys = (float) Math.sin(rad) * this.power;
    }

    @Override
    public void step() {
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.ys > 7 || this.gei_flg == 2) {
            float splitRot = (float) Math.toDegrees(Math.atan2(this.ys, this.xs));
            for (int i = 0; i < 7; i++) {
                float atkRot = splitRot - 30 + 10 * i;
                float rad = (float) Math.toRadians(atkRot);
                new Bullet(this.x, this.y, this.side).setVecR(rad, 20).setGravity(0);
            }
            kill();
            return;
        }
        if (this.y > 570) {
            kill();
            return;
        }
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        this.ys = this.ys + 0.32F;
    }
}
