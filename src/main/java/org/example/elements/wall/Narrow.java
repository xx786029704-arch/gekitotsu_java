package org.example.elements.wall;

import org.example.Main;
import org.example.elements.Wall;

public class Narrow extends Wall {
    public int cnt;
    public int speed;
    private boolean mode = true;

    public Narrow(float X, float Y, int S, int TYPE) {
        super(X, Y, S, TYPE);
        speed = 16;
        cnt = 8;
        Main.turn_cw[side].addShape(this);
    }

    public void kill() {
        super.kill();
        if (mode){
            Main.turn_cw[side].removeShape(this);
        }
        else {
            Main.turn_ccw[side].removeShape(this);
        }
    }

    public void stepEx(){   //额外行为
        cnt++;
        if (cnt == speed) {
            cnt = 0;
            if (mode) {
                Main.turn_cw[side].removeShape(this);
                Main.turn_ccw[side].addShape(this);
            }
            else {
                Main.turn_ccw[side].removeShape(this);
                Main.turn_cw[side].addShape(this);
            }
            mode = !mode;
        }
    }
}
