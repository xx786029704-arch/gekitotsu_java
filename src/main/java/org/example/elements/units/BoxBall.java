package org.example.elements.units;

import org.example.GameTask;
import org.example.elements.Ball;
import org.example.elements.wall.Box;

public class BoxBall extends Ball {
    public BoxBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        speed = 200;
        hp = 15;
        max_hp = 15;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (cnt == speed && jump_flg != 1 && game.fort[on_side].hitTestPoint(x + cos_rot * 38, y + sin_rot * 38)) {
            cnt--;
        }
        else if (cnt == speed + 4) {
            cnt = 0;
            new Box(game,x + cos_rot * 38, y + sin_rot * 38, on_side, 0, jump_flg == 2 ? -1 : on_side);
        }
    }
}
