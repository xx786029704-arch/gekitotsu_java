package org.example.elements.wall;

import org.example.Main;
import org.example.elements.Wall;
import org.example.elements.hit.HitsJump;

public class Near extends Wall {
    public Near(float X, float Y, int S, int TYPE) {
        super(X, Y, S, TYPE);
    }

    public void stepEx(){
        if (Main.norikomi_flg) {
            new HitsJump(x, y - 35, Main.jump_u[side]);
        }
    }
}
