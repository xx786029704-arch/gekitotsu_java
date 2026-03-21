package org.example.elements.atk;

import org.example.Main;
import org.example.Shape;
import org.example.elements.Ball;
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
        if (this.y > 570 || this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            hit();
            return;
        }
        move();
    }

    @Override
    public boolean hit() {   //触发击退
        new HitsDrop(this.x, this.y, Main.atk[this.side]);
        List<Shape> targets = new ArrayList<>(Main.unit[1 - this.side].getShapes());
        for (Shape shape : targets) {
            if (shape instanceof Ball target) {
                float dx = this.x - target.x;
                float dy = this.y - target.y;
                if (dx * dx + dy * dy <= 900F) {
                    target.y = target.y + this.ys * 2;
                    target.x = target.x + this.xs * 2;
                    target.xySync();
                    if (target.y >= 566) {
                        target.y = 566;
                    }
                    if (target.x < 0 || target.x > 1920) {
                        target.kill();
                        kill();
                        return false;
                    }
                }
            }
        }
        kill();
        return false;
    }
}
