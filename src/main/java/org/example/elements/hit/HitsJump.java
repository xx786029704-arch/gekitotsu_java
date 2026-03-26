package org.example.elements.hit;

import org.example.CompositeShape;
import org.example.Game;
import org.example.ShapeBuilder;

public class HitsJump extends CompositeShape {
    private final CompositeShape parent;
    protected final Game game;

    public HitsJump(Game game, float X, float Y, CompositeShape s) {
        super(X, Y);
        this.game = game;
        ShapeBuilder.into(this)
                .roundedRectangle(-17.15F,-17.5F,34.35F,35F,4F);
        id = this.game.addElement(this);
        parent = s;
        s.addShape(this);
    }

    @Override
    public void step(){
        this.game.elements.remove(id);
        parent.removeShape(this);
    }

    @Override
    public boolean hitTestPoint(float X, float Y){
        float dx = X - x + 0.3F;
        float dy = Y - y;
        if (dx < -16.85F || dx > 17.5F || dy < -17.5F || dy > 17.5F){
            return false;
        }
        if (dx > -12.85F){
            if (dx > 13.5F){
                dx -= 13.5F;
            } else {
                return true;
            }
        } else {
            dx += 12.85F;
        }
        if (dy > -13.5F){
            if (dy > 13.5F){
                dy -= 13.5F;
            } else {
                return true;
            }
        } else {
            dy += 13.5F;
        }
        return dx * dx + dy * dy <= 16F;
    }
}
