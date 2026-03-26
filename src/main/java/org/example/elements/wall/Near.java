package org.example.elements.wall;

import org.example.Game;
import org.example.elements.Wall;
import org.example.elements.hit.HitsJump;

public class Near extends Wall {
    public Near(Game game, float X, float Y, int S, int TYPE) {
        super(game, X, Y, S, TYPE);
    }

    public void stepEx(){
        if (this.game.norikomi_flg) {
            new HitsJump(this.game, x, y - 35, this.game.jump_u[side]);
        }
    }
}
