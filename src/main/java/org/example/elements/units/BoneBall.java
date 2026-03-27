package org.example.elements.units;

import org.example.GameTask;
import org.example.elements.Ball;
import org.example.elements.hit.HitsBone;
import org.example.elements.hit.HitsKen;

public class BoneBall extends Ball {     //骨玉
    public BoneBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        speed = 30;
    }

    @Override
    public void stepEx(){
        if (this.cnt == this.speed) {
            this.cnt = 0;
            new HitsBone(game, x, y, rot, side, id, cos_rot, sin_rot);
        }
    }
}
