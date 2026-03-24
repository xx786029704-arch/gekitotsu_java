package org.example.elements.atk;

import org.example.Main;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBombMult;

public class TobiBombBullet extends Bullet {   //飞玉落弹
    public TobiBombBullet(float X, float Y, int S) {   //初始化
        super(X, Y, S);
        power = 5;
        gravity = 0.32F;
    }

    @Override
    public void step() {   //每帧逻辑
        if (y > 570 || Main.team[1 - side].hitTestPoint(x, y) || gei_flg == 2) {
            new HitsBombMult(x, y, side, 0.5F);
            kill();
            return;
        }
        move();
    }
}
