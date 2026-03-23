package org.example;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class CompositeShape extends Shape {     //复合形状类

    //可能的优化：也许可以换成链表
    private final List<Shape> shapes = new ArrayList<>();   //所有包含的子形状

    /*
     这里可以直接用{{x, y, r}, {x, y, r}...}的二维数组存储所有的圆而不是用子形状
     然后修改下面的点碰撞判定，可以减少对象数量（不再需要圆形子形状）
     但是原版没有特别复杂的复合形状，仅可用来优化要塞核心，车板等少数形状
     后续如果有需求可以优化
     */

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
    public void moveTo(float X, float Y) {
        for (Shape s : shapes) {
            s.move(X - this.x, Y - this.y);
        }
        super.moveTo(X, Y);
    }

    @Override
    public void moveTo(float X, float Y) {
        for (Shape s : shapes) {
            s.move(X - this.x, Y - this.y);
        }
        super.moveTo(X, Y);
    }

    @Override
    public Boolean hitTestPoint(float X, float Y){
        for (Shape s : shapes){
            if (s.hitTestPoint(X, Y))
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