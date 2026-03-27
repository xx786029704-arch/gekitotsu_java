package org.example.elements.units;

import org.example.GameTask;
import org.example.elements.Ball;
import org.example.elements.hit.HitsKen;
import org.example.elements.hit.HitsKnight;

import java.awt.*;
import java.awt.geom.Path2D;

public class KnightBall extends TateBall {     //骑玉
    public KnightBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        hp = 50;
        max_hp = 50;
        speed = 7;
    }

    @Override
    public void stepEx(){
        if (this.cnt == this.speed) {
            this.cnt = 0;
            new HitsKnight(game, x, y, rot, side, id, cos_rot, sin_rot);
        }
    }
}
