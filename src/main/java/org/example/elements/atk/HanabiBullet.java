package org.example.elements.atk;

import org.example.Main;
import org.example.elements.Bullet;
public class HanabiBullet extends Bullet {   //花玉子弹
    private int cnt = 0;

    public HanabiBullet(float X, float Y, int S) {   //初始化
        super(X, Y, S);
        this.gei_flg = 1;
        this.gravity = 0;
        this.r = this.r * 0.8F;
    }

    @Override
    public void step() {   //每帧逻辑
        this.cnt++;
        if (this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (this.y > 570 || Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2 || this.cnt > 60) {
            hit();
            return;
        }
        move();
    }

    @Override
    public boolean hit() {   //触发散射
        for (int i = 0; i < 20; i++) {
            float rot = this.rot + 18F * i;
            new HinokoBullet(this.x, this.y, this.side, rot);
        }
        return super.hit();
    }
}
