package org.example.elements.wall;

import org.example.GameTask;
import org.example.elements.Wall;

public class Iron extends Wall {
    public Iron(GameTask GAME, float X, float Y, int S, int TYPE) {
        super(GAME, X, Y, S, TYPE);
        max_hp = 150;
        hp = 150;
    }
}
