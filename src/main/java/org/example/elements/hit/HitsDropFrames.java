package org.example.elements.hit;

import org.example.CompositeShape;
import org.example.Main;
import org.example.Round;

public class HitsDropFrames extends Round {   //基础攻击判定（黑线）
    private final CompositeShape parent;
    private int frames;
    private final int id;

    public HitsDropFrames(float X, float Y, CompositeShape S, int frame, float scale) {
        super(X, Y, 15.5F * scale);
        xySync();
        this.frames = frame;
        id = Main.addElement(this);
        S.addShape(this);
        parent = S;
    }

    @Override
    public void step(){
        frames--;
        if (frames <= 0) {
            Main.elements.remove(id);
            parent.removeShape(this);
        }
    }
}
