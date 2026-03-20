package org.example.elements;

import org.example.Main;
import org.example.elements.hit.HitsDrop;

public class HinokoBullet extends Bullet {   //花玉小子弹
    private int cnt = 0;
    private float power = 30F;

    public HinokoBullet(float X, float Y, int S, float rotation) {   //初始化
        super(X, Y, S);
        this.rot = rotation;
        this.gei_flg = 1;
        this.gravity = 0;
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * this.power;
        this.ys = (float) Math.sin(rad) * this.power;
        this.x = X + (float) Math.cos(rad) * 14F;
        this.y = Y + (float) Math.sin(rad) * 14F;
    }

    @Override
    public void step() {   //每帧逻辑
        this.cnt++;
        if (this.y > 570 || this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.cnt > 20 || this.gei_flg == 2) {
            new HitsDrop(this.x, this.y, Main.atk[this.side]);
            kill();
            return;
        }
        this.power = this.power * 0.8F;
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * this.power;
        this.ys = (float) Math.sin(rad) * this.power;
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
    }
}
