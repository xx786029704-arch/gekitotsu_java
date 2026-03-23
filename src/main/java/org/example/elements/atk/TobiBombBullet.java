package org.example.elements.atk;

import org.example.Main;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBombMult;

public class TobiBombBullet extends Bullet {   //飞玉落弹
    public TobiBombBullet(float X, float Y, int S) {   //初始化
        super(X, Y, S);
        this.gei_flg = 1;
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y > 570 || Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            new HitsBombMult(this.x, this.y, this.side, 0.5F);
            kill();
            return;
        }
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        this.xySync();
        this.ys = this.ys + 0.32F;
    }
}
