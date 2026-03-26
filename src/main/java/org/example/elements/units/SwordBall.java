package org.example.elements.units;

import org.example.Game;
import org.example.elements.Ball;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsKen;

public class SwordBall extends Ball {     //剑玉
    public SwordBall(Game game, float X, float Y, int R, int S, int TYPE) {
        super(game, X, Y, R, S, TYPE);
        hp = 30;
        max_hp = 30;
        speed = 10;
    }

    @Override
    public void stepEx(){
        if (this.cnt == this.speed) {
            this.cnt = 0;
            new HitsKen(this.game, x, y, rot, side, id, cos_rot, sin_rot);
        }
    }
}
