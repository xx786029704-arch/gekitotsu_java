package org.example.elements.units;

import org.example.Game;
import org.example.Shape;
import org.example.elements.Ball;
import org.example.elements.Bullet;
import org.example.elements.atk.GeiBullet;

public class HanBall extends Ball {   //迎玉
    public int t_id = -1;

    public HanBall(Game game, float X, float Y, int R, int S, int TYPE) {   //初始化
        super(game, X, Y, R, S, TYPE);
        hp = 15;
        max_hp = 15;
        speed = 20;
    }

    @Override
    public void stepEx() {
        if (cnt > speed + 5 && t_id == -1 && jump_flg != 1) {
            float t_dist2 = 4225;
            float dist2;
            float dx;
            float dy;
            float dot;
            for (Shape s : this.game.atk[1-side].getShapes()) {
                if (s instanceof Bullet bullet && bullet.gei_flg == 1) {
                    dx = bullet.x - x;
                    dy = bullet.y - y;
                    if (dx > 65 || dy > 65 || dx < -65 || dy < -65){
                        continue;
                    }
                    dist2 = dx * dx + dy * dy;
                    dot = dx * cos_rot + dy * sin_rot;
                    if (dot < 0 || dot * dot < dist2 * 0.8213938048432696F){
                        continue;
                    }
                    if (dist2 <= t_dist2) {
                        t_dist2 = dist2;
                        t_id = bullet.id;
                    }
                }
            }
            if (t_id > -1) {
                ((Bullet) this.game.elements.get(t_id)).reflect(rot);
                cnt = 0;
                t_id = -1;
            }
        }
    }

    @Override
    public void hurt(boolean is_crash){
        cnt = 0;
        t_id = -1;
    }
}
