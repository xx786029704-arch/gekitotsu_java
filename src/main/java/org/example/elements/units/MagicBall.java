package org.example.elements.units;

import org.example.GameTask;
import org.example.elements.Ball;
import org.example.elements.hit.HitsKabe;
import org.example.elements.hit.MagicLaser;
import org.example.Utils;

public class MagicBall extends Ball {   //魔玉
    public boolean shooting;
    public MagicBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        shooting = false;
        speed = 150;
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
                new MagicLaser(game, x + 38.F * Utils.cos((int) rot), y + 38.F * Utils.sin((int) rot), rot, side, id);
            }
            if (cnt >= speed + 38) {
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