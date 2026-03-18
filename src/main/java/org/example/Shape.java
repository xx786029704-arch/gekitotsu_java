package org.example;

import java.awt.*;

public abstract class Shape {   //最基本的抽象形状类
    public float x;
    public float y;
    public int id;

    public Shape(float X, float Y) {
        this.x = X;
        this.y = Y;
    }

    public abstract Boolean hitTestPoint(float X, float Y);     //点碰撞

    public void move(float X, float Y) {
        x += X;
        y += Y;
    }

    public void step(){     //每帧执行的代码
    }

    public abstract void draw(Graphics2D g2d);
}
