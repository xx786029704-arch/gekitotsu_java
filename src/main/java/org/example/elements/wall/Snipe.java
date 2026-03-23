package org.example.elements.wall;

import org.example.Main;
import org.example.elements.Wall;

public class Snipe extends Wall {
    public Snipe(float X, float Y, int S, int TYPE) {
        super(X, Y, S, TYPE);
        Main.snipe[side].addShape(this);
        max_hp = 50;
        hp = 50;
    }

    public void kill() {
        super.kill();
        Main.snipe[side].removeShape(this);
    }
}
