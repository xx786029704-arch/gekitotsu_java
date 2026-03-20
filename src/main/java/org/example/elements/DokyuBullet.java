package org.example.elements;

import org.example.Main;
import org.example.elements.hit.HitsDrop;

public class DokyuBullet extends Bullet {   //弩玉子弹
    private final float power;

    public DokyuBullet(float X, float Y, int S, float rotation, float power) {   //初始化
        super(X, Y, S);
        this.rot = rotation;
        this.power = power;
        this.gei_flg = 1;
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * this.power;
        this.ys = (float) Math.sin(rad) * this.power;
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y > 570) {
            kill();
            return;
        }
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            new HitsDrop(this.x, this.y, Main.atk[this.side]);
            kill();
            return;
        }
        this.rot = (float) Math.toDegrees(Math.atan2(this.ys, this.xs));
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        this.ys = this.ys + 0.32F;
    }
}
