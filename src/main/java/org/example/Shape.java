package org.example;

import java.awt.*;

public abstract class Shape {
    public float x;
    public float y;

    public Shape(float X, float Y) {
        this.x = X;
        this.y = Y;
    }

    public abstract Boolean HitTestPoint(float X, float Y);

    public void move(float X, float Y) {
        x += X;
        y += Y;
    }

    public void step(){
    }

    public abstract void draw(Graphics2D g2d);
}
