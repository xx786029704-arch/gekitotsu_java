package org.example.elements.wall;

import org.example.GameTask;
import org.example.elements.Wall;

public class Wide extends Wall {
    public int cnt;
    public int speed;
    private boolean mode = false;

    public Wide(GameTask GAME, float X, float Y, int S, int TYPE) {
        super(GAME, X, Y, S, TYPE);
        speed = 40;
        cnt = 20;
        game.turn_ccw[side].addShape(this);
    }

    public void kill() {
        super.kill();
        if (mode){
            game.turn_cw[side].removeShape(this);
        }
        else {
            game.turn_ccw[side].removeShape(this);
        }
    }

    public void stepEx(){
        cnt++;
        if (cnt == speed) {
            cnt = 0;
            if (mode) {
                game.turn_cw[side].removeShape(this);
                game.turn_ccw[side].addShape(this);
            }
            else {
                game.turn_ccw[side].removeShape(this);
                game.turn_cw[side].addShape(this);
            }
            mode = !mode;
        }
    }
}
