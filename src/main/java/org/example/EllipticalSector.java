package org.example;

import java.awt.*;

public class EllipticalSector extends Shape {     //椭圆扇形类

    public float r;
    public float dirX;
    public float dirY;
    public float cosHalfAngle;   //半角余弦值提前算好，点碰撞不需要重复调用
    public float dir;
    public float a;
    public float flatness;

    /**
     * @param X x
     * @param Y y
     * @param R 椭圆 x轴半径
     * @param angle 还是圆的时候的角度(沿y轴方向复原成圆)
     * @param direction 还是圆的时候的角度(沿y轴方向复原成圆)
     * @param _flatness 扁平度(椭圆的 a/b)
     */

    public EllipticalSector(float X, float Y, float R, float angle, float direction, float _flatness){
        super(X, Y);
        r = R;
        flatness = _flatness;
        a = angle * 0.01745329252F;
        dir = direction * 0.01745329252F;
        dirX = (float)Math.cos(dir);
        dirY = (float)Math.sin(dir);
        cosHalfAngle = (float)Math.cos(angle * 0.5f);
    }

    public EllipticalSector(float X, float Y, float R, float angle, float direction, float _flatness, float _dirX, float _dirY, float _cosHalfAngle){
        this(X, Y, R, angle, direction, _flatness);
        dirX = _dirX;
        dirY = _dirY;
        cosHalfAngle = _cosHalfAngle;
    }

    @Override
    public Boolean hitTestPoint(float X, float Y){
        float dx = X - x;
        float dy = (Y - y) * flatness;
        if (dx > r || dy > r || dx < -r || dy < -r){
            return false;
        }
        float dist2 = dx * dx + dy * dy;
        if (dist2 > r * r)
            return false;
        float dot = dx * dirX + dy * dirY;
        float cos2 = cosHalfAngle * cosHalfAngle;
        return dot > 0 && dot * dot >= dist2 * cos2;
    }

    public void update() {  //扇形范围变化时（如剑气）更新数据
        dirX = (float)Math.cos(dir);
        dirY = (float)Math.sin(dir);
        cosHalfAngle = (float)Math.cos(a * 0.5f);
    }

    @Override
    public void draw(Graphics2D g2d) {
        float wrk = 0.01745329252F;
        int startAngle = (int) (-(dir + a / 2) / wrk);
        int arcAngle = (int) (a / wrk);
        g2d.drawArc((int)(x - r), (int)(y - r / flatness), (int)(r * 2), (int)(r * 2 / flatness), startAngle, arcAngle);
        g2d.drawLine((int)x, (int)y, (int)(x + r * Math.cos(dir + a/2)), (int)(y + r * Math.sin(dir + a/2) / flatness));
        g2d.drawLine((int)x, (int)y, (int)(x + r * Math.cos(dir - a/2)), (int)(y + r * Math.sin(dir- a/2) / flatness));
    }
}