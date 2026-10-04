package org.example.elements.units;

import org.example.GameTask;
import org.example.Shape;
import org.example.elements.Ball;
import org.example.elements.Bullet;
import org.example.elements.atk.GeiBullet;

public class GeiBall extends Ball {   //迎玉
    public int t_id = -1;

    public GeiBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        hp = 15;
        max_hp = 15;
        speed = 40;
    }

    @Override
    public void stepEx() {
        if (cnt > speed + 5 && t_id == -1 && jump_flg != 1) {
            float t_dist2 = 90601;
            float dist2;
            float dx;
            float dy;
            float dot;
            for (Shape s : game.atk[1-side].getShapes()) {
                if (s instanceof Bullet bullet && bullet.gei_flg == 1) {
                    dx = bullet.x - x;
                    dy = bullet.y - y;
                    if (dx > 301 || dy > 301 || dx < -301 || dy < -301){
                        continue;
                    }
                    dist2 = dx * dx + dy * dy;
                    dot = dx * cos_rot + dy * sin_rot;
                    if (dot < 0 || dot * dot < dist2 * 0.75F){
                        continue;
                    }
                    if (dist2 <= t_dist2) {
                        t_dist2 = dist2;
                        t_id = bullet.id;
                    }
                }
            }
            if (t_id > -1) {
                ((Bullet) game.elements.get(t_id)).gei_flg = 3;
                new GeiBullet(game, id, t_id);
            }
        }
    }

    @Override
    public void hurt(boolean is_crash){
        cnt = 0;
        t_id = -1;
    }

}
