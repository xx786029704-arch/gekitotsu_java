package org.example.elements.hit;

import org.example.CompositeShape;
import org.example.Main;
import org.example.Round;

public class HitsDrop extends Round {   //基础攻击判定（黑线）
    public CompositeShape parent;

    public HitsDrop(float X, float Y, CompositeShape S) {
        super(X, Y, 15.5F);
        xySync();
        id = Main.addElement(this);
        S.addShape(this);
        parent = S;
    }

    @Override
    public void step(){
        Main.elements.remove(id);
        parent.removeShape(this);
    }
}
