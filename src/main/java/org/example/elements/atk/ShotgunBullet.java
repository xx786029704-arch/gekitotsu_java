package org.example.elements.atk;

import org.example.Main;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsDrop;

public class ShotgunBullet extends Bullet {
    private int cnt = 0;

    public ShotgunBullet(float X, float Y, int S) {
        super(X, Y, S);
        this.x = X;
        this.y = Y;
    }

    @Override
    public void step() {
        this.cnt++;
        if (this.y > 570 || this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2 || this.cnt > 7) {
            new HitsDrop(this.x, this.y, Main.atk[this.side]);
            kill();
            return;
        }
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
    }
}
