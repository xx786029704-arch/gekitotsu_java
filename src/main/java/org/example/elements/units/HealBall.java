package org.example.elements.units;

import org.example.Game;
import org.example.elements.Ball;
import org.example.elements.hit.HitsHeal;

public class HealBall extends Ball {   //愈玉
    public HealBall(Game game, float X, float Y, int R, int S, int TYPE) {   //初始化
        super(game, X, Y, R, S, TYPE);
        speed = 150;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed + 4) {
            this.cnt = 0;
            float healX = this.x + cos_rot * 120;
            float healY = this.y + sin_rot * 120;
            new HitsHeal(this.game, healX, healY, this.game.heal[this.side], 100F);
        }
    }
}
