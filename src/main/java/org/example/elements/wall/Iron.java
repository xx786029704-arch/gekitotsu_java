package org.example.elements.wall;

import org.example.elements.Wall;
import org.example.Game;

public class Iron extends Wall {
    public Iron(Game game, float X, float Y, int S, int TYPE) {
        super(game, X, Y, S, TYPE);
        max_hp = 150;
        hp = 150;
    }
}
