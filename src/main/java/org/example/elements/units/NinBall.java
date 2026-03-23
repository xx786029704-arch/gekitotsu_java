package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.atk.NinBullet;

public class NinBall extends Ball {   //忍玉
    public NinBall(float X, float Y, int R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        speed = 70;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed + 3) {
            this.cnt = 0;
            new NinBullet(this.x, this.y, this.side).setVecMult(cos_rot, sin_rot, 18).move();
        }
    }
}
