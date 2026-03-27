package org.example.elements.wall;

import org.example.GameTask;
import org.example.elements.Wall;

public class Snipe extends Wall {
    public Snipe(GameTask GAME, float X, float Y, int S, int TYPE) {
        super(GAME, X, Y, S, TYPE);
        game.snipe[side].addShape(this);
        max_hp = 50;
        hp = 50;
    }

    public void kill() {
        super.kill();
        game.snipe[side].removeShape(this);
    }
}
