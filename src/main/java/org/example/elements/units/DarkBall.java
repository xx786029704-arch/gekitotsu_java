package org.example.elements.units;

import org.example.Main;
import org.example.elements.Ball;
import org.example.elements.wall.Box;

public class DarkBall extends Ball {
    public DarkBall(float X, float Y, int R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 200;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (cnt == speed + 4) {
            cnt = 0;
            BoneBall summon = new BoneBall(x + cos_rot * 38, y + sin_rot * 38, rot, side, 31);
            summon.cnt = 0;
            summon.jump_flg = 1;
            summon.xs = cos_rot * 20;
            summon.ys = sin_rot * 20;
            summon.on_side = 1 - this.side;
        }
    }
}
