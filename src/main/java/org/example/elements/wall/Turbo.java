package org.example.elements.wall;

import org.example.Main;
import org.example.elements.Wall;

public class Turbo extends Wall {
    public Turbo(float X, float Y, int S, int TYPE) {
        super(X, Y, S, TYPE);
        max_hp = 150;
        hp = 150;
        Main.bases[S].axl+=2;
    }

    public void kill() {
        super.kill();
        Main.bases[side].axl-=2;
    }
}
