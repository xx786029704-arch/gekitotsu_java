package org.example.elements.atk;

import org.example.Main;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsDrop;

public class KanBullet extends Bullet {   //贯玉子弹
    private static final float SPEED = 40F;

    public KanBullet(float X, float Y, int S, float rotation) {   //初始化
        super(X, Y, S);
        this.rot = rotation;
        float rad = (float) Math.toRadians(rotation);
        this.xs = (float) Math.cos(rad) * SPEED;
        this.ys = (float) Math.sin(rad) * SPEED;
        this.x = X + this.xs;
        this.y = Y + this.ys;
        xySync();
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y > 570 || this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (Main.atk[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            hit();
            return;
        }
        if (Main.fort[1 - this.side].hitTestPoint(this.x, this.y) || Main.shield[1 - this.side].hitTestPoint(this.x, this.y)) {
            new HitsDrop(this.x, this.y, Main.atk[this.side]);
        }
        move();
    }
}
