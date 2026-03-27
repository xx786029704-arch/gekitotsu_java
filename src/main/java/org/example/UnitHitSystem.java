package org.example;

import org.example.elements.Ball;
import org.example.elements.Bullet;

public class UnitHitSystem extends HitSystem{
    public UnitHitSystem(float X, float Y) {
        super(X, Y);
    }

    public void removeShape(Shape s) {
        shapes.remove(s);
        left.remove(s);
        right.remove(s);
        common.remove(s);
    }

    @Override
    public void resign(){
        common.clear();
        left.clear();
        right.clear();
        for (Shape s : shapes){
            if (s instanceof Ball ball){
                if (ball.x < mid + 42){
                    left.add(ball);
                }
                if (ball.x > mid - 42){
                    right.add(ball);
                }
                continue;
            }
            common.add(s);
        }
    }

    @Override
    public boolean hitTestPoint(float X, float Y) {
        for (Shape s : common){
            if (s.hitTestPoint(X, Y))
                return true;
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
}
