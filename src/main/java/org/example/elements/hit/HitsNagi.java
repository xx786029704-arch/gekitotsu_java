package org.example.elements.hit;

import org.example.Main;
import org.example.Sector;
import org.example.Shape;

import java.awt.*;

public class HitsNagi extends Sector {
    private final int side;
    private int frame;
    private final int user;
    private final boolean flipped;
    private final float cos_rot;
    private final float sin_rot;
    @Deprecated
    private final float rot_radius;

    public HitsNagi(float X, float Y, int R, int S, int USER, float _cos_rot, float _sin_rot) {
        super(X, Y, 63, 45, 292.5F);
        xySync();
        frame = 0;
        user = USER;
        side = S;
        cos_rot = _cos_rot;
        sin_rot = _sin_rot;
        rot_radius = R * 0.017453292519943295F;
        flipped = R >= 90 + side && R <= 270 + side;
        id = Main.addElement(this);
        Main.atk[side].addShape(this);
    }

    public void kill() {
        Main.elements.remove(id);
        Main.atk[side].removeShape(this);
    }

    @Override
    public void step(){
        Shape shape = Main.elements.get(user);
        if (shape != null){
            this.x = shape.x;
            this.y = shape.y;
        }
        if (!Main.ENABLE_VISUALIZATION) {
            if (frame++ == 9){
                kill();
            }
            return;
        }
        switch (frame++){
            case 0, 1:{
                a += 45 * 0.017453292519943295F;
                dir += 22.5F * 0.017453292519943295F;
                dirX = (float)Math.cos(dir);
                dirY = (float)Math.sin(dir);
                cosHalfAngle = (float)Math.cos(a * 0.5f);
                break;
            }
            case 2:{
                dir = 22.5F * 0.017453292519943295F;
                dirX = (float)Math.cos(dir);
                dirY = (float)Math.sin(dir);
                break;
            }
            case 3, 4, 5, 6:{
                dir += 45F * 0.017453292519943295F;
                dirX = (float)Math.cos(dir);
                dirY = (float)Math.sin(dir);
                break;
            }
            case 7, 8:{
                a -= 45 * 0.017453292519943295F;
                dir += 22.5F * 0.017453292519943295F;
                dirX = (float)Math.cos(dir);
                dirY = (float)Math.sin(dir);
                cosHalfAngle = (float)Math.cos(a * 0.5f);
                break;
            }
            case 9:{
                kill();
                break;
            }
        }
    }

    @Override
    public boolean hitTestPoint(float X, float Y){
        if (X - x > 81.4F || Y - y > 81.4F || X - x < -81.4F || Y - y < -81.4F){
            return false;
        }
        float dx = ((X - x) * cos_rot + (Y - y) * sin_rot);
        float dy = ((Y - y) * cos_rot - (X - x) * sin_rot);
        if (!flipped){
            dy = -dy;
        }
        dx -= 18.4F;

        switch (frame){
            case 0:{
                if (dx * dy < dx * dx) return false;
                break;
            }
            case 1:{
                if (dx * dy < 0F) return false;
                break;
            }
            case 2, 6:{
                if (dx * dy < -dx * dx) return false;
                break;
            }
            case 3, 7:{
                if (dx * dy > dx * dx) return false;
                break;
            }
            case 4:{
                if (dx * dy > dy * dy) return false;
                break;
            }
            case 5:{
                if (dx * dy < -dy * dy) return false;
                break;
            }
            case 8:{
                if (dx * dy > 0F) return false;
                break;
            }
            case 9:{
                if (dx * dy > -dx * dx) return false;
                break;
            }

        }
        return dx * dx + dy * dy <= 3969F;
    }

    @Override
    public void draw(Graphics2D g2d){
        if (flipped) {
            g2d.translate(0, y);
            g2d.scale(1, -1);
            g2d.translate(0, -y);
        }
        g2d.rotate(flipped ? -rot_radius : rot_radius, (int) x, (int) y);
        float wrk = 0.017453292519943295F;
        int startAngle = (int) (-(dir + a / 2) / wrk);
        int arcAngle = (int) (a / wrk);
        float biased = x + 18.4F;
        g2d.drawArc((int)(biased - r), (int)(y - r), (int)(r * 2), (int)(r * 2), startAngle, arcAngle);
        g2d.drawLine((int)biased, (int)y, (int)(biased + r * Math.cos(dir + a/2)), (int)(y + r * Math.sin(dir + a/2)));
        g2d.drawLine((int)biased, (int)y, (int)(biased + r * Math.cos(dir - a/2)), (int)(y + r * Math.sin(dir- a/2)));
        startAngle += 180;
        g2d.drawArc((int)(biased - r), (int)(y - r), (int)(r * 2), (int)(r * 2), startAngle, arcAngle);
        g2d.drawLine((int)biased, (int)y, (int)(biased - r * Math.cos(dir + a/2)), (int)(y - r * Math.sin(dir + a/2)));
        g2d.drawLine((int)biased, (int)y, (int)(biased - r * Math.cos(dir - a/2)), (int)(y - r * Math.sin(dir- a/2)));
        g2d.rotate(flipped ? rot_radius : -rot_radius, (int) x, (int) y);
        if (flipped) {
            g2d.translate(0, y);
            g2d.scale(1, -1);
            g2d.translate(0, -y);
        }
    }
}
