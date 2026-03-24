package org.example;

import java.awt.*;

public class Rectangle extends Shape {   //矩形类
    private final float width;
    private final float height;
    private final float start_x;
    private final float start_y;

    public Rectangle(float X, float Y, float startX, float startY, float W, float H) {   //左上角与宽高
        super(X, Y);
        width = W;
        height = H;
        start_x = startX;
        start_y = startY;
    }

    @Override
    public boolean hitTestPoint(float X, float Y) {   //点碰撞
        X -= start_x;
        Y -= start_y;
        return X >= x && X <= x + width && Y >= y && Y <= y + height;
    }

    @Override
    public void draw(Graphics2D g2d) {   //绘制
        g2d.drawRect((int) (x + start_x), (int) (y + start_y), (int) width, (int) height);
    }
}
