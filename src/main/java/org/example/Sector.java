package org.example;

import java.awt.*;

public class Sector extends Shape {     //扇形类

    public float r;
    public float dirX;
    public float dirY;
    public float cosHalfAngle;   //半角余弦值提前算好，点碰撞不需要重复调用
    public float dir;
    public float a;

    public Sector(float X, float Y, float R, float angle, float direction){
        super(X, Y);
        r = R;
        a = angle * 0.017453292519943295F;
        dir = direction * 0.017453292519943295F;
        dirX = (float)Math.cos(dir);
        dirY = (float)Math.sin(dir);
        cosHalfAngle = (float)Math.cos(angle * 0.5f);
    }

    @Override
    public Boolean hitTestPoint(float X, float Y){
        if (X - x > r || Y - y > r || x - X > r || y - Y > r){
            return false;
        }
        float dx = X - this.x;
        float dy = Y - this.y;
        float dist2 = dx * dx + dy * dy;
        if (dist2 > r * r)
            return false;
        float dot = dx * dirX + dy * dirY;
        float cos2 = cosHalfAngle * cosHalfAngle;
        return dot > 0 && dot * dot >= dist2 * cos2;
    }

    public void update() {  //扇形范围变化时（如薙玉剑气）更新数据
        dirX = (float)Math.cos(dir);
        dirY = (float)Math.sin(dir);
        cosHalfAngle = (float)Math.cos(a * 0.5f);
    }

    @Override
    public void draw(Graphics2D g2d) {
        float wrk = 0.017453292519943295F;
        int startAngle = (int) (-(dir + a / 2) / wrk);
        int arcAngle = (int) (a / wrk);
        g2d.drawArc((int)(x - r), (int)(y - r), (int)(r * 2), (int)(r * 2), startAngle, arcAngle);
        g2d.drawLine((int)x, (int)y, (int)(x + r * Math.cos(dir + a/2)), (int)(y + r * Math.sin(dir + a/2)));
        g2d.drawLine((int)x, (int)y, (int)(x + r * Math.cos(dir - a/2)), (int)(y + r * Math.sin(dir- a/2)));
    }
}
