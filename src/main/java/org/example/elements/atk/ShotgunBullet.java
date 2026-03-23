package org.example.elements.atk;

import org.example.Main;
import org.example.Utils;
import org.example.elements.Bullet;
public class ShotgunBullet extends Bullet {   //散玉霰弹
    private int cnt = 0;

    public ShotgunBullet(float X, float Y, int S, int rotation) {   //初始化
        super(X, Y, S);
        float power = 27F + (float) (Math.random() * 7F);
        this.xs = Utils.cos(rotation) * power;
        this.ys = Utils.sin(rotation) * power;
    }

    @Override
    public void step() {   //每帧逻辑
        this.cnt++;
        if (this.y > 570 || this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2 || this.cnt > 7) {
            hit();
            return;
        }
        move();
    }
}
