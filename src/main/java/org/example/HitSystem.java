package org.example;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;

public abstract class HitSystem extends CompositeShape{
    public static int mid = 960;
    public ArrayList<Shape> upper = new ArrayList<>();
    public ArrayList<Shape> left = new ArrayList<>();
    public ArrayList<Shape> right = new ArrayList<>();
    public ArrayList<Shape> common = new ArrayList<>();

    public HitSystem(float X, float Y) {
        super(X, Y);
    }

    public void addShape(Shape s) {
        shapes.add(s);
        common.add(s);
    }

    public void removeShape(Shape s) {
        shapes.remove(s);
        upper.remove(s);
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
            for (Shape s : upper){
                if (s.hitTestPoint(X, Y))
                    return true;
            }
            return false;
        }
        if (X <= mid){
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

    @Override
    public void draw(Graphics2D g2d) {
    }

    public abstract void resign();
}
