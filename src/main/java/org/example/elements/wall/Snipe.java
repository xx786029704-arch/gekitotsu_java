package org.example.elements.wall;

import org.example.Game;
import org.example.elements.Wall;

public class Snipe extends Wall {
    public Snipe(Game game, float X, float Y, int S, int TYPE) {
        super(game, X, Y, S, TYPE);
        this.game.snipe[side].addShape(this);
        max_hp = 50;
        hp = 50;
    }

    public void kill() {
        super.kill();
        this.game.snipe[side].removeShape(this);
    }
}
