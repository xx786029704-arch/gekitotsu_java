package org.example;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CompositeShape extends Shape {

    private final List<Shape> shapes = new CopyOnWriteArrayList<>();

    public CompositeShape(float X, float Y) {
        super(X, Y);
    }

    public void addShape(Shape s) {
        shapes.add(s);
    }

    public void removeShape(Shape s) {
        shapes.remove(s);
    }

    public List<Shape> getShapes() {
        return shapes;
    }

    @Override
    public void move(float X, float Y){
        super.move(X, Y);
        for (Shape s : shapes){
            s.move(X,Y);
        }
    }

    @Override
    public Boolean HitTestPoint(float X, float Y){
        float localX = X - this.x;
        float localY = Y - this.y;
        for (Shape s : shapes){
            if (s.HitTestPoint(localX, localY))
                return true;
        }
        return false;
    }

    @Override
    public void draw(Graphics2D g2d) {
        for (Shape s : shapes) {
            s.draw(g2d);
        }
    }
}