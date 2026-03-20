package org.example.elements;

import org.example.Main;
import org.example.elements.hit.HitsDrop;

public class ShotgunBullet extends Bullet {   //散玉霰弹
    private int cnt = 0;

    public ShotgunBullet(float X, float Y, int S, float rotation) {   //初始化
        super(X, Y, S);
        this.rot = rotation;
        this.gei_flg = 1;
        float power = 27F + (float) (Math.random() * 7F);
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * power;
        this.ys = (float) Math.sin(rad) * power;
        this.gravity = 0;
        this.x = X;
        this.y = Y;
    }

    @Override
    public void step() {   //每帧逻辑
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
