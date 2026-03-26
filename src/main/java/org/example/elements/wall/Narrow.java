package org.example.elements.wall;

import org.example.Game;
import org.example.elements.Wall;

public class Narrow extends Wall {
    public int cnt;
    public int speed;
    private boolean mode = true;

    public Narrow(Game game, float X, float Y, int S, int TYPE) {
        super(game, X, Y, S, TYPE);
        speed = 16;
        cnt = 8;
        this.game.turn_cw[side].addShape(this);
    }

    public void kill() {
        super.kill();
        if (mode){
            this.game.turn_cw[side].removeShape(this);
        }
        else {
            this.game.turn_ccw[side].removeShape(this);
        }
    }

    public void stepEx(){
        cnt++;
        if (cnt == speed) {
            cnt = 0;
            if (mode) {
                this.game.turn_cw[side].removeShape(this);
                this.game.turn_ccw[side].addShape(this);
            }
            else {
                this.game.turn_ccw[side].removeShape(this);
                this.game.turn_cw[side].addShape(this);
            }
            mode = !mode;
        }
    }
}
