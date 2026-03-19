package org.example.elements.atk;

import org.example.Main;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsDrop;

public class HaneBullet extends Bullet {
    private int bounceCount = 0;

    public HaneBullet(float X, float Y, int S) {
        super(X, Y, S);
        this.r *= 0.8F;
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
        this.x += this.xs;
        this.y += this.ys;
    }
}
