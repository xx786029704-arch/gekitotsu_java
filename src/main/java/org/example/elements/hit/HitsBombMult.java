package org.example.elements.hit;

import org.example.GameTask;
import org.example.Main;
import org.example.Round;

public class HitsBombMult extends Round {   //暴风（倍率）
    private int frame;
    private final int side;
    private final float mult;
    private final GameTask game;

    public HitsBombMult(GameTask GAME, float X, float Y, int S, float mult) {
        super(X, Y, 30 * mult);
        xySync();
        game = GAME;
        this.side = S;
        this.mult = mult;
        this.id = game.addElement(this);
        game.atk[side].addShape(this);
    }

    @Override
    public void step() {
        if (frame < 3) {
            this.r = frame == 0 ? 60 : (frame == 1 ? 39 : 12);  //硬编码省去new float[]开销
            this.r *= mult;
            frame++;
        }else {
            game.elements.remove(id);
            game.atk[side].removeShape(this);
        }
    }
}
