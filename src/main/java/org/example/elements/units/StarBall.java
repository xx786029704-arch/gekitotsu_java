package org.example.elements.units;

import org.example.GameTask;
import org.example.Utils;
import org.example.elements.Ball;
import org.example.elements.hit.StarPrepareLaser;

public class StarBall extends Ball {   //星玉
    public boolean shooting;
    public StarBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        shooting = false;
        speed = 250;
        hp = 10;
        max_hp = 10;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (jump_flg == 1) {
            shooting = false;
        } else {
            if (cnt == speed + 1) {
                shooting = true;
                new StarPrepareLaser(game, x + 28.F * Utils.cos((int) rot), y + 28.F * Utils.sin((int) rot), rot, side, id);
            }
            if (cnt >= speed + 31) {
                cnt = 0;
                shooting = false;
            }
        }
    }

    @Override
    public void hurt(boolean is_crash) {
        shooting = false;
    }
}