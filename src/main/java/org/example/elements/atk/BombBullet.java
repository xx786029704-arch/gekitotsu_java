package org.example.elements.atk;

import org.example.Main;
import org.example.Round;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;

public class BombBullet extends Bullet {   //爆玉子弹
    public BombBullet(float X, float Y, int S) {   //初始化
        super(X, Y, S);
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y > 570 || Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            new HitsBomb(this.x, this.y, this.side);
            kill();
            return;
        }
        move();
    }
}
