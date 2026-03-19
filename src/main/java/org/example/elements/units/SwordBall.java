package org.example.elements.units;

import org.example.Main;
import org.example.elements.Ball;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsKen;

public class SwordBall extends Ball {     //剑玉
    public SwordBall(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 10;
    }

    @Override
    public void stepEx(){
        if (this.cnt == this.speed)
        {
            this.cnt = 0;
            new HitsKen(x, y, rot, side, id);
        }
    }
}
