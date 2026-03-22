package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.hit.HitsNagi;

import java.awt.*;

public class NagiBall extends Ball {     //薙玉
    public NagiBall(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        hp = 30;
        max_hp = 30;
        speed = 20;
    }

    @Override
    public void stepEx(){
        if (this.cnt == this.speed)
        {
            this.cnt = 0;
            new HitsNagi(x, y, rot, side, id, cos_rot, sin_rot);
        }
    }

    @Override
    public void draw(Graphics2D g2d){
        super.draw(g2d);
    }
}
