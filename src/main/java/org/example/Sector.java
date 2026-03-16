package org.example;

import java.awt.*;

public class Sector extends Shape {

    public float r;
    public float dirX;
    public float dirY;
    public float cosHalfAngle;

    public Sector(float X, float Y, float R, float angle, float direction){
        super(X, Y);
        r = R;
        dirX = (float)Math.cos(direction);
        dirY = (float)Math.sin(direction);
        cosHalfAngle = (float)Math.cos(angle * 0.5f);
    }

    @Override
    public Boolean HitTestPoint(float X, float Y){
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

    @Override
    public void draw(Graphics2D g2d) {
        float direction = (float) Math.atan2(dirY, dirX);
        float angle = (float) Math.acos(cosHalfAngle) * 2;
        int startAngle = (int) Math.toDegrees(-(direction + angle / 2));
        int arcAngle = (int) Math.toDegrees(angle);
        g2d.drawArc((int)(x - r), (int)(y - r), (int)(r * 2), (int)(r * 2), startAngle, arcAngle);
        g2d.drawLine((int)x, (int)y, (int)(x + r * Math.cos(direction + angle/2)), (int)(y + r * Math.sin(direction + angle/2)));
        g2d.drawLine((int)x, (int)y, (int)(x + r * Math.cos(direction - angle/2)), (int)(y + r * Math.sin(direction - angle/2)));
    }
}