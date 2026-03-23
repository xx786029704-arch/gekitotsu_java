package org.example.elements.hit;

import org.example.CompositeShape;
import org.example.Main;
import org.example.Round;

public class HitsHeal extends Round {   //愈玉和缮玉的治疗判定
    private final CompositeShape parent;
    private final int id;

    public HitsHeal(float X, float Y, CompositeShape S, float radius) {
        super(X, Y, radius);
        xySync();
        this.id = Main.addElement(this);
        S.addShape(this);
        this.parent = S;
    }

    @Override
    public void step(){
        Main.elements.remove(id);
        parent.removeShape(this);
    }
}
