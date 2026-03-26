package org.example.elements.units;

import org.example.Game;
import org.example.elements.Ball;

public class SaiBall extends Ball {
    public SaiBall(Game game, float X, float Y, int R, int S, int TYPE) {
        super(game, X, Y, R, S, TYPE);
        speed = 200;
        hp = 15;
        max_hp = 15;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (cnt == speed + 5) {
            cnt = 0;
            this.game.saihai_cnt[side] = 2;
            this.game.saihai_rot[side] = rot;
        }
    }
}
