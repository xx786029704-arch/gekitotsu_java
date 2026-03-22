package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.Bullet;

public class TargetBall extends Ball {     //的玉
    public TargetBall(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        hp = 30;
        max_hp = 30;
    }
}
