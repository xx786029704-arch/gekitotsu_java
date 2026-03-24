package org.example.elements.units;

import org.example.Main;
import org.example.elements.Ball;
import org.example.elements.atk.HolyBullet;

public class HolyBall extends Ball {
    public HolyBall(float X, float Y, int R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 300;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (cnt > speed + 5 && Main.dead_last[side] != 0 && jump_flg != 1) {
            cnt = 0;
            new HolyBullet(x + cos_rot * 38, y + sin_rot * 38, Main.dead_last[side], rot, side, on_side, jump_flg);
            Main.dead_last[side] = 0;
        }
    }

    @Override
    public void hurt(boolean is_crash){
        cnt = 0;
    }
}
