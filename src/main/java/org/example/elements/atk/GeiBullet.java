package org.example.elements.atk;

import org.example.Main;
import org.example.Shape;
import org.example.elements.Ball;
import org.example.elements.Bullet;
import org.example.elements.units.GeiBall;

import java.awt.*;

public class GeiBullet extends Shape {
    private final int user_id;
    private final int target_id;

    public GeiBullet(int USER, int TARGET) {
        super(0, 0);
        user_id = USER;
        target_id = TARGET;
        id = Main.addElement(this);
    }

    public void kill() {
        Main.elements.remove(id);
    }

    @Override
    public void step(){
        Shape user = Main.elements.getOrDefault(user_id, null);
        if (user == null){
            kill();
            return;
        }
        Shape target = Main.elements.getOrDefault(target_id, null);
        if (target == null) {
            ((GeiBall) user).cnt = 0;
            ((GeiBall) user).t_id = -1;
            this.kill();
            return;
        }

        ((Bullet) target).gei_flg = 2;
        if (((GeiBall) user).type > 0 && ((GeiBall) user).t_id == -1) {
            this.kill();
        }
    }

    @Override
    public boolean hitTestPoint(float X, float Y) {
        return false;
    }

    @Override
    public void draw(Graphics2D g2d) {
    }
}
