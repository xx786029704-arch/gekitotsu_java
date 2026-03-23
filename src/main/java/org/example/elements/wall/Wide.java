package org.example.elements.wall;

import org.example.Main;
import org.example.elements.Wall;
import org.example.elements.hit.HitsJump;

public class Wide extends Wall {
    public int cnt;
    public int speed;
    private boolean mode = false;

    public Wide(float X, float Y, int S, int TYPE) {
        super(X, Y, S, TYPE);
        speed = 40;
        cnt = 20;
        Main.turn_ccw[side].addShape(this);
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

    public void stepEx(){
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
