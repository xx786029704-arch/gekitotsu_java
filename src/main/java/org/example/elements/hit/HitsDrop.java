package org.example.elements.hit;

import org.example.CompositeShape;
import org.example.GameTask;
import org.example.Main;
import org.example.Round;

public class HitsDrop extends Round {   //基础攻击判定（黑线）
    public CompositeShape parent;
    private final GameTask game;

    public HitsDrop(GameTask GAME, float X, float Y, CompositeShape S) {
        super(X, Y, 15.5F);
        game = GAME;
        xySync();
        id = game.addElement(this);
        S.addShape(this);
        parent = S;
    }

    @Override
    public void step(){
        game.elements.remove(id);
        parent.removeShape(this);
    }
}
