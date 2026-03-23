package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.atk.HaneBullet;

public class HaneBall extends Ball {   //跳玉
    public HaneBall(float X, float Y, float R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        speed = 50;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed) {
            this.cnt = 0;
            new HaneBullet(this.x, this.y, this.side).setVecMult(cos_rot, sin_rot, 18).move();
        }
    }
}
