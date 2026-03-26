package org.example.elements.hit;

import org.example.CompositeShape;
import org.example.Game;
import org.example.Round;

public class HitsHeal extends Round {   //愈玉和缮玉的治疗判定
    private final CompositeShape parent;
    private final int id;
    protected final Game game;

    public HitsHeal(Game game, float X, float Y, CompositeShape S, float radius) {
        super(X, Y, radius);
        this.game = game;
        xySync();
        this.id = this.game.addElement(this);
        S.addShape(this);
        this.parent = S;
    }

    @Override
    public void step(){
        this.game.elements.remove(id);
        parent.removeShape(this);
    }
}
