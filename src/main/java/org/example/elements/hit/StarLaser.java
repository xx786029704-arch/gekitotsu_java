package org.example.elements.hit;

import org.example.Main;
import org.example.Utils;
import org.example.elements.units.MagicBall;

public class StarLaser extends LaserBase {     //星玉的激光

    private int cnt;

    public StarLaser(float X, int S) {
        super(X, -600.f, 90, S, 25.F, 15.5F);
        this.cnt = 0;
    }

    @Override
    protected void follow() {
        this.cnt++;
        if (this.cnt >= 45) {
            kill();
        }
    }

}