package org.example.elements.hit;

import org.example.CompositeShape;
import org.example.Game;
import org.example.Round;

public class HitsDropFrames extends Round {   //基础攻击判定（黑线）
    private final CompositeShape parent;
    private int frames;
    private final int id;
    protected final Game game;

    public HitsDropFrames(Game game, float X, float Y, CompositeShape S, int frame, float scale) {
        super(X, Y, 15.5F * scale);
        this.game = game;
        xySync();       //大概率可以删掉，不太确定
        this.frames = frame;
        id = this.game.addElement(this);
        S.addShape(this);
        parent = S;
    }

    @Override
    public void step(){
        frames--;
        if (frames <= 0) {
            this.game.elements.remove(id);
            parent.removeShape(this);
        }
    }
}
