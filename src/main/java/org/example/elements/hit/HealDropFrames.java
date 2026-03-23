package org.example.elements.hit;

import org.example.CompositeShape;
import org.example.Main;
import org.example.Round;

public class HealDropFrames extends Round {   //愈玉和缮玉的治疗判定
    private final CompositeShape parent;
    private final int id;
    private final int bornTime;

    public HealDropFrames(float X, float Y, CompositeShape S, float radius) {
        super(X, Y, radius);
        xySync();
        this.id = Main.addElement(this);
        S.addShape(this);
        this.parent = S;
        this.bornTime = Main.time;
    }

    @Override
    public void step(){
        if (Main.time > bornTime) {
            Main.elements.remove(id);
            parent.removeShape(this);
        }
    }
}
