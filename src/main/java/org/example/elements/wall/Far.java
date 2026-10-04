package org.example.elements.wall;

import org.example.GameTask;
import org.example.elements.Wall;
import org.example.elements.hit.HitsJump;

public class Far extends Wall {
    public int cnt;
    public int speed;

    public Far(GameTask GAME, float X, float Y, int S, int TYPE) {
        super(GAME, X, Y, S, TYPE);
        speed = 900;
        int int_x = (int) x;
        cnt = side == 0 ? (int_x - 50) % speed : (380 - (int_x - 1490)) % speed;
    }

    public void stepEx(){
        cnt++;
        if (cnt == speed) {
            cnt = 0;
            new HitsJump(game, x, y - 35, game.jump_f[side]);
        }
    }
}
