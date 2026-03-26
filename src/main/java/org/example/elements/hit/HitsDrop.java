package org.example.elements.hit;

import org.example.CompositeShape;
import org.example.Game;
import org.example.Round;

public class HitsDrop extends Round {   //基础攻击判定（黑线）
    private final CompositeShape parent;
    protected final Game game;

    public HitsDrop(Game game, float X, float Y, CompositeShape S) {
        super(X, Y, 15.5F);
        this.game = game;
        xySync();
        id = this.game.addElement(this);
        S.addShape(this);
        parent = S;
    }

    @Override
    public void step(){
        this.game.elements.remove(id);
        parent.removeShape(this);
    }
}
