package org.example.elements.units;

import org.example.Game;
import org.example.elements.Ball;
import org.example.elements.Bullet;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

public class TateBall extends Ball {     //盾玉
    private final boolean flipped;
    public int hurt_time = 0;
    @Deprecated
    private float rot_radius;

    public TateBall(Game game, float X, float Y, int R, int S, int TYPE) {
        super(game, X, Y, R, S, TYPE);
        hp = 30;
        max_hp = 30;
        rot_radius = (float) Math.toRadians(R);
        flipped = rot >= 90 + side && rot <= 270 + side;
    }

    @Override
    public void hurt(boolean is_crash){
        if (!is_crash){
            hurt_time = 3;
        }
    }

    @Override
    public void stepEx(){
        if(hurt_time > 0){
            hurt_time--;
        }
    }

    @Override
    public boolean hitTestPoint(float X, float Y){
        float dx = X - x;
        float dy = Y - y;
        if (dx > 54F || dy > 54 || dx < -54F || dy < -54F){
            return false;
        } else {
            dx = ((X - x) * cos_rot + (Y - y) * sin_rot);
            dy = ((Y - y) * cos_rot - (X - x) * sin_rot);
            if (flipped){
                dy = -dy;
            }
        }
        if (hurt_time > 0){
            if (dx > 26.75F || dy > 25.45 || dx < -43.4F || dy < -44.75F){
                return false;
            }
            if (dx * dx + dy * dy <= 647.7025F){
                return true;
            } else {
                float dx2 = dx - 20.1F;
                float dy2 = dy + 22.5F;
                if (dx2 * dx2 + dy2 * dy2 <= 43.23F) {
                    return true;
                } else if (
                        (dx + 31.25F) * 0.9 - (dy + 43.45F) * 22.55F <= 0 &&
                        (dx + 8.7F) * 41.65F + (dy + 42.55F) * 24.05F <= 0 &&
                        -(dx + 32.75F) * 18.35F + (dy + 0.9F) * 12.35F <= 0 &&
                        -(dx + 45.1F) * 24.2F - (dy + 19.25F) * 13.85F <= 0){
                    return true;
                } else {
                    dx2 = dx + 20.5F;
                    dy2 = dy + 22.5F;
                    return dx2 * dx2 + dy2 * dy2 <= 43.23F;
                }
            }
        } else {
            if (dx > 55F || dy > 25.2 || -dx > r || dy < -23.8F){
                return false;
            }
            if (dx >= 16.15F){
                dx -= 16.15F;
                dy -= 0.7F;
                return  dx * dx * 600.25F + dy * dy * 1509.3225F <= 905970.830625F;
            }
            else {
                return dx * dx + dy * dy <= r * r;
            }
        }
    }

    @Override
    public void draw(Graphics2D g2d) {
        if (hurt_time > 0){
            g2d.drawOval((int)(x - 25.45F), (int)(y - 25.45F) , 50, 50);
            if (flipped) {
                g2d.translate(0, y);
                g2d.scale(1, -1);
                g2d.translate(0, -y);
            }
            g2d.rotate(flipped ? -rot_radius : rot_radius, (int) x, (int) y);
            float[][] verts = {{x - 31.25F, y - 43.45F}, {x - 8.7F, y - 42.55F}, {x - 32.75F, y - 0.9F}, {x - 45.1F, y - 19.25F}};
            Path2D.Float path = new Path2D.Float();
            path.moveTo(verts[0][0], verts[0][1]);
            for (int i = 1; i < verts.length; i++) {
                path.lineTo(verts[i][0], verts[i][1]);
            }
            path.closePath();
            g2d.draw(path);
            g2d.drawOval((int)(x - 27F), (int)(y - 29F), 13, 13);
            g2d.drawOval((int)(x + 13.5F), (int)(y - 29F), 13, 13);
            g2d.rotate(flipped ? rot_radius : -rot_radius, (int) x, (int) y);
            if (flipped) {
                g2d.translate(0, y);
                g2d.scale(1, -1);
                g2d.translate(0, -y);
            }
        }else {
            g2d.drawOval((int)(x - r), (int)(y - r), (int)(r * 2), (int)(r * 2));
            if (flipped){
                g2d.translate(0, y);
                g2d.scale(1, -1);
                g2d.translate(0, -y);
            }
            g2d.rotate(flipped ? -rot_radius : rot_radius, (int) x, (int) y);
            g2d.drawArc((int)(x - 22.85F), (int)(y - 23.8F), 78, 49, -90, 180);
            g2d.drawLine((int) (x + 16.15F), (int)(y - 23.8F), (int) (x + 16.15F), (int)(y + 25.2F));
            g2d.rotate(flipped ? rot_radius : -rot_radius, (int) x, (int) y);
            if (flipped){
                g2d.translate(0, y);
                g2d.scale(1, -1);
                g2d.translate(0, -y);
            }
        }
    }
}
