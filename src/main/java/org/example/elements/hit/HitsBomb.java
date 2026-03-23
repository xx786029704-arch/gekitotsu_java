package org.example.elements.hit;

import org.example.Main;
import org.example.Round;

public class HitsBomb extends Round {   //暴风（无倍率）
    private int frame;
    private final int side;

    public HitsBomb(float X, float Y, int S) {
        super(X, Y, 30);
        xySync();
        this.side = S;
        this.id = Main.addElement(this);
        Main.atk[side].addShape(this);
    }

    @Override
    public void step() {
        if (frame < 3) {
            this.r = frame == 0 ? 60 : (frame == 1 ? 39 : 12);  //硬编码省去new float[]开销
            frame++;
        }else {
            Main.elements.remove(id);
            Main.atk[side].removeShape(this);
        }
    }
}
