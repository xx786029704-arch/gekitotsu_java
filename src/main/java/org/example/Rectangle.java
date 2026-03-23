package org.example;

import java.awt.*;

public class Rectangle extends Shape {   //矩形类
    public float width;
    public float height;

    public Rectangle(float X, float Y, float W, float H) {   //左上角与宽高
        super(X, Y);
        this.width = W;
        this.height = H;
    }

    @Override
    public boolean hitTestPoint(float X, float Y) {   //点碰撞
        float minX = Math.min(this.x, this.x + this.width);
        float maxX = Math.max(this.x, this.x + this.width);
        float minY = Math.min(this.y, this.y + this.height);
        float maxY = Math.max(this.y, this.y + this.height);
        return X >= minX && X <= maxX && Y >= minY && Y <= maxY;
    }

    @Override
    public void draw(Graphics2D g2d) {   //绘制
        float minX = Math.min(this.x, this.x + this.width);
        float maxX = Math.max(this.x, this.x + this.width);
        float minY = Math.min(this.y, this.y + this.height);
        float maxY = Math.max(this.y, this.y + this.height);
        g2d.drawRect((int) minX, (int) minY, (int) (maxX - minX), (int) (maxY - minY));
    }
}
