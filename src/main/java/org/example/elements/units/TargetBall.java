package org.example.elements.units;

import org.example.Game;
import org.example.elements.Ball;
public class TargetBall extends Ball {
    public TargetBall(Game game, float X, float Y, int R, int S, int TYPE) {
        super(game, X, Y, R, S, TYPE);
        hp = 30;
        max_hp = 30;
    }
}
