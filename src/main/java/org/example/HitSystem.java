package org.example;

import java.awt.*;
import java.util.ArrayList;

public abstract class HitSystem extends CompositeShape{
    public ArrayList<Shape> upper_left = new ArrayList<>();
    public ArrayList<Shape> upper_right = new ArrayList<>();
    public ArrayList<Shape> left = new ArrayList<>();
    public ArrayList<Shape> right = new ArrayList<>();
    public ArrayList<Shape> common = new ArrayList<>();
    protected final GameTask game;

    public HitSystem(GameTask GAME, float X, float Y) {
        super(X, Y);
        game = GAME;
    }

    public void addShape(Shape s) {
        shapes.add(s);
        common.add(s);
    }

    public void removeShape(Shape s) {
        shapes.remove(s);
        upper_left.remove(s);
        upper_right.remove(s);
        left.remove(s);
        right.remove(s);
        common.remove(s);
    }

    @Override
    public boolean hitTestPoint(float X, float Y) {
        for (Shape s : common){
            if (s.hitTestPoint(X, Y))
                return true;
        }
        if (Y < 0){
            if (X <= game.mid) {
                for (Shape s : upper_left) {
                    if (s.hitTestPoint(X, Y))
                        return true;
                }
                return false;
            } else {
                for (Shape s : upper_right) {
                    if (s.hitTestPoint(X, Y))
                        return true;
                }
                return false;
            }
        }
        if (X <= game.mid){
            for (Shape s : left){
                if (s.hitTestPoint(X, Y))
                    return true;
            }
            return false;
        }
        for (Shape s : right){
            if (s.hitTestPoint(X, Y))
                return true;
        }
        return false;
    }

    public abstract void resign();
}
