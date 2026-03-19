package org.example.elements;

import org.example.Main;
import org.example.elements.hit.HitsDrop;

public class HaneBullet extends Bullet {
    private int bounceCount = 0;

    public HaneBullet(float X, float Y, int S, float rotation) {
        super(X, Y, S);
        this.rot = rotation;
        this.gei_flg = 1;
        this.r = this.r * 0.8F;
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * 18F;
        this.ys = (float) Math.sin(rad) * 18F;
        this.x = X + this.xs;
        this.y = Y + this.ys;
    }

    @Override
    public void step() {
        if (this.y > 570 || this.y < -600) {
            this.ys = -this.ys;
            this.bounceCount++;
        }
        if (this.x > 1920 || this.x < 0) {
            this.xs = -this.xs;
            this.bounceCount++;
        }
        if (this.bounceCount > 4) {
            kill();
            return;
        }
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            new HitsDrop(this.x, this.y, Main.atk[this.side]);
            kill();
            return;
        }
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
    }
}
