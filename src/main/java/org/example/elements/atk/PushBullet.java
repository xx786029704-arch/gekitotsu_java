package org.example.elements.atk;

import org.example.Main;
import org.example.Shape;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsDrop;

import java.util.ArrayList;
import java.util.List;

public class PushBullet extends Bullet {   //押玉叭↑叭↓
    public PushBullet(float X, float Y, int S) {   //初始化
        super(X, Y, S);
    }

    @Override
    public void step() {   //每帧逻辑
        if (y > 570 || y < -600 || x > 1920 || x < 0) {
            kill();
            return;
        }
        if (Main.team[1 - side].hitTestPoint(x, y) || gei_flg == 2) {
            hit();
            return;
        }
        move();
    }

    @Override
    public boolean hit() {   //触发击退
        new HitsDrop(x, y, Main.atk[side]);
        List<Shape> targets = new ArrayList<>(Main.unit[1 - side].getShapes());
        for (Shape target : targets) {
            float dx = x - target.x;
            float dy = y - target.y;
            if (dx * dx + dy * dy <= 900F) {
                target.y = target.y + ys * 2;
                target.x = target.x + xs * 2;
                target.xySync();
                if (target.y >= 566) {
                    target.y = 566;
                }
                if (target.x < 0 || target.x > 1920) {
                    Main.elements.remove(target.id);
                    Main.unit[1-side].removeShape(target);
                    kill();
                    return false;
                }
            }
        }
        kill();
        return false;
    }
}
