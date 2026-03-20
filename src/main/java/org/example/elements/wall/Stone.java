package org.example.elements.wall;

import org.example.elements.Wall;

public class Stone extends Wall {
    public Stone(float X, float Y, int S, int TYPE) {
        super(X, Y, S, TYPE);
        max_hp = 75;
        hp = 75;
    }
}
