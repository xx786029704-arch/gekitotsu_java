package org.example.elements.atk;

import org.example.Main;
import org.example.Round;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;

public class BombBullet extends Bullet {
    public BombBullet(float X, float Y, int S) {
        super(X, Y, S);
        gei_flg = 1;
    }

    @Override
    public void step() {
        if (this.y > 570 || Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            new HitsBomb(this.x, this.y, this.side);
            kill();
            return;
        }
        move();
    }
}
