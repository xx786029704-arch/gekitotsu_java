package org.example.elements.atk;

import org.example.Main;
import org.example.elements.Bullet;
public class HinokoBullet extends Bullet {   //花玉小子弹
    private int cnt = 0;

    public HinokoBullet(float X, float Y, int S, float rotation) {   //初始化
        super(X, Y, S);
        this.gei_flg = 1;
        this.gravity = 0;
        float rot_radius = rotation * 0.01745329252F;
        float cos_rot = (float) Math.cos(rot_radius);
        float sin_rot = (float) Math.sin(rot_radius);
        this.xs = cos_rot * 30;
        this.ys = sin_rot * 30;
        this.x = X + cos_rot * 14F;
        this.y = Y + sin_rot * 14F;
    }

    @Override
    public void step() {   //每帧逻辑
        this.cnt++;
        if (this.y > 570 || this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.cnt > 20 || this.gei_flg == 2) {
            hit();
            return;
        }
        this.xs *= 0.8F;
        this.ys *= 0.8F;
        move();
    }
}
