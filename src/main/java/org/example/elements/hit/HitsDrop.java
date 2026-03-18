package org.example.elements.hit;

import org.example.CompositeShape;
import org.example.Main;
import org.example.Round;

public class HitsDrop extends Round {   //基础攻击判定（黑线）
    CompositeShape parent;

    public HitsDrop(float X, float Y, CompositeShape S) {
        super(X, Y, 15.5F);
        id = Main.addElement(this);
        S.addShape(this);
        parent = S;
    }

    public void kill() {
        Main.elements.remove(id);
        parent.removeShape(this);
    }

    @Override
    public void step(){
        kill();
    }
}
