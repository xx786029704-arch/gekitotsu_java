package org.example.elements.wall;

import org.example.GameTask;
import org.example.Main;
import org.example.elements.Wall;
import org.example.elements.hit.HitsJump;

public class Near extends Wall {
    public Near(GameTask GAME, float X, float Y, int S, int TYPE) {
        super(GAME, X, Y, S, TYPE);
    }

    public void stepEx(){
        if (game.norikomi_flg) {
            new HitsJump(game, x, y - 35, game.jump_u[side]);
        }
    }
}
