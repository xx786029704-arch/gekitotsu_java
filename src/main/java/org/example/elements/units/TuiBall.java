package org.example.elements.units;

import org.example.GameTask;
import org.example.elements.Ball;
import org.example.elements.atk.TuiBullet;

public class TuiBall extends Ball {   //坠玉
    public TuiBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        hp = 10;
        max_hp = 10;
        speed = 300;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (cnt == speed + 3) {
            cnt = 0;
            new TuiBullet(game, x + cos_rot * 35, y + sin_rot * 35, side, cos_rot, sin_rot);
        }
    }
}
