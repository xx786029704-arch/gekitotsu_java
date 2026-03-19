package org.example.elements.atk;

import org.example.Main;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsDrop;

public class NinBullet extends Bullet {
    private int bounceCount = 0;

    public NinBullet(float X, float Y, int S) {
        super(X, Y, S);
        this.r = 12.4F;
    }

    @Override
    public void step() {
        if (this.x > 1920) {
            if (this.side != 1) {
                this.bounceCount = 99;
            }
            else {
                this.x = 0;
                this.bounceCount++;
            }
        }
        else if (this.x < 0) {
            if (this.side != 0) {
                this.bounceCount = 99;
            }
            else {
                this.x = 1920;
                this.bounceCount++;
            }
        }
        if (this.bounceCount > 1) {
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
