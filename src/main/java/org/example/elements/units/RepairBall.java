package org.example.elements.units;

import org.example.GameTask;
import org.example.elements.Ball;
import org.example.elements.hit.HitsHeal;

public class RepairBall extends Ball {   //缮玉
    public RepairBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        speed = 150;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed + 4) {
            this.cnt = 0;
            float repairX = this.x + cos_rot * 120;
            float repairY = this.y + sin_rot * 120;
            new HitsHeal(game, repairX, repairY, game.repair[this.side], 100F);
        }
    }
}
