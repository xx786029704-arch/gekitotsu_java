package org.example.elements.wall;

import org.example.elements.Wall;
import org.example.Game;

public class Stone extends Wall {
    public Stone(Game game, float X, float Y, int S, int TYPE) {
        super(game, X, Y, S, TYPE);
        max_hp = 75;
        hp = 75;
    }
}
