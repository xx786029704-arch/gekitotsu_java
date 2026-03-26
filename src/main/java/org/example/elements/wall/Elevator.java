package org.example.elements.wall;

import org.example.Game;
import org.example.elements.Wall;
import org.example.elements.hit.HitsJump;

public class Elevator extends Wall {
    private int ys = 1;

    public Elevator(Game game, float X, float Y, int S, int TYPE) {
        super(game, X, Y, S, TYPE);
    }

    public void stepEx(){   //额外行为
        boolean block_flg = false;
        float _x = x;
        float _y = y;
        x += 100;
        y += 100;
        if (ys == 1 && this.game.fort[side].hitTestPoint(_x, _y + 15)) {
            block_flg = true;
            ys = -1;
        }
        else if (ys == -1 && (this.game.wall[side].hitTestPoint(_x, _y - 48) || _y <= this.game.bases[side].y - 380)) {
            block_flg = true;
            ys = 1;
        }
        if (!block_flg) {
            move(0, ys);
        }
        x -= 100;
        y -= 100;
    }
}
