package org.example;

import java.awt.*;

public class Round extends Shape{   //圆形类
    public float r;

    public Round(float X, float Y, float R) {
        super(X, Y);
        this.r = R;
    }

    @Override
    public boolean hitTestPoint(float X, float Y){
        if (X - x > r || Y - y > r || x - X > r || y - Y > r){
            return false;
        }
        float dx = this.x - X;
        float dy = this.y - Y;
        return dx * dx + dy * dy <= r * r;
    }
}
