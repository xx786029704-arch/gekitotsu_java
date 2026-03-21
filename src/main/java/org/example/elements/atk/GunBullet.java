package org.example.elements.atk;

import org.example.Main;
import org.example.elements.Bullet;
public class GunBullet extends Bullet {   //机玉子弹
    public GunBullet(float X, float Y, int S) {   //初始化
        super(X, Y, S);
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y > 570 || this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            hit();
            return;
        }
        move();
    }
}
