package org.example.elements.units;

import org.example.Game;
import org.example.elements.Ball;
import org.example.elements.Bullet;

public class BowBall extends Ball {     //弓玉
    public BowBall(Game game, float X, float Y, int R, int S, int TYPE) {
        super(game, X, Y, R, S, TYPE);
        speed = 30;
    }

    @Override
    public void stepEx(){
        if (this.cnt == this.speed + 6) {
            this.cnt = 0;
            new Bullet(this.game, this.x, this.y, this.side).setVecMult(cos_rot, sin_rot, 15).setGravity(0.32F);
        }
    }
}
