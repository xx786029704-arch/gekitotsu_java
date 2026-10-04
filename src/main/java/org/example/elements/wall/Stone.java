package org.example.elements.wall;

import org.example.GameTask;
import org.example.elements.Wall;

public class Stone extends Wall {
    public Stone(GameTask GAME, float X, float Y, int S, int TYPE) {
        super(GAME, X, Y, S, TYPE);
        max_hp = 75;
        hp = 75;
    }
}
