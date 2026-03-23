package org.example.elements.units;

import org.example.Main;
import org.example.elements.Ball;

public class SaiBall extends Ball {
    public SaiBall(float X, float Y, int R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 200;
        hp = 15;
        max_hp = 15;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (cnt == speed + 5) {
            cnt = 0;
            Main.saihai_cnt[side] = 2;
            Main.saihai_rot[side] = rot;
        }
    }
}
