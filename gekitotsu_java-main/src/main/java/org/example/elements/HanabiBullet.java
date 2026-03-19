package org.example.elements;

import org.example.Main;
import org.example.elements.hit.HitsDrop;

public class HanabiBullet extends Bullet {
    private int cnt = 0;

    public HanabiBullet(float X, float Y, int S, float rotation) {
        super(X, Y, S);
        this.rot = rotation;
        this.gei_flg = 1;
        this.gravity = 0;
        this.r = this.r * 0.8F;
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * 10F;
        this.ys = (float) Math.sin(rad) * 10F;
        this.x = X + this.xs;
        this.y = Y + this.ys;
    }

    @Override
    public void step() {
        this.cnt++;
        if (this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (this.y > 570 || Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2 || this.cnt > 60) {
            new HitsDrop(this.x, this.y, Main.atk[this.side]);
            for (int i = 0; i < 20; i++) {
                float rot = this.rot + 18F * i;
                new HinokoBullet(this.x, this.y, this.side, rot);
            }
            kill();
            return;
        }
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
    }
}
