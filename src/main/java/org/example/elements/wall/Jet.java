package org.example.elements.wall;

import org.example.Main;
import org.example.elements.Wall;

public class Jet extends Wall {
    public Jet(float X, float Y, int S, int TYPE) {
        super(X, Y, S, TYPE);
        max_hp = 15;
        hp = 15;
        Main.bases[S].axl+=1;
    }

    public void kill() {
        super.kill();
        Main.bases[side].axl-=1;
    }
}
