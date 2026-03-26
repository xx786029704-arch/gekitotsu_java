package org.example.elements.units;

import org.example.Game;
import org.example.elements.Ball;
import org.example.elements.atk.RetsuBullet;

public class RetsuBall extends Ball {   //裂玉
    public RetsuBall(Game game, float X, float Y, int R, int S, int TYPE) {   //初始化
        super(game, X, Y, R, S, TYPE);
        speed = 120;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed + 1) {
            this.cnt = 0;
            new RetsuBullet(this.game, this.x, this.y - 10, this.side).setVecMult(cos_rot, sin_rot, 15).setGravity(0.32F);
        }
    }
}
