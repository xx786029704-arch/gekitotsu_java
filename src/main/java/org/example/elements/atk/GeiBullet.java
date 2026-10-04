package org.example.elements.atk;

import org.example.GameTask;
import org.example.Main;
import org.example.Shape;
import org.example.elements.Ball;
import org.example.elements.Bullet;
import org.example.elements.units.GeiBall;

import java.awt.*;

public class GeiBullet extends Shape {
    private final int user_id;
    private final int target_id;
    private final GameTask game;

    public GeiBullet(GameTask GAME, int USER, int TARGET) {
        super(0, 0);
        game = GAME;
        user_id = USER;
        target_id = TARGET;
        id = game.addElement(this);
    }

    public void kill() {
        game.elements.remove(id);
    }

    @Override
    public void step(){
        Shape user = game.elements.get(user_id);
        if (user == null){
            kill();
            return;
        }
        Shape target = game.elements.get(target_id);
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
}
