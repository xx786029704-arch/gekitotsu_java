package org.example.elements.wall;

import org.example.elements.Wall;

public class Iron extends Wall {
    public Iron(float X, float Y, int S, int TYPE) {
        super(X, Y, S, TYPE);
        max_hp = 150;
        hp = 150;
    }
}
