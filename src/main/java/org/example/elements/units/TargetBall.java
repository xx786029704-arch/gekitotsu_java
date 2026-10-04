package org.example.elements.units;

import org.example.GameTask;
import org.example.elements.Ball;
public class TargetBall extends Ball {
    public TargetBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        hp = 30;
        max_hp = 30;
    }
}
