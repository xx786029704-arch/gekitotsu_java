package org.example.elements.atk;

import org.example.GameTask;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;

public class MissileBullet extends Bullet {   //导玉导弹
    private int cnt = 0;
    private float speed = 4F;
    private float rot;

    public MissileBullet(GameTask GAME, float X, float Y, int S, int rotation) {   //初始化
        super(GAME, X, Y, S);
        rot = rotation;
        gei_flg = 1;
        updateVelocity();
    }

    @Override
    public void step() {   //每帧逻辑
        cnt++;
        if (y < -1200 || x > 2560 || x < -640) {
            kill();
            return;
        }
        if (game.team[1 - side].hitTestPoint(x, y) || y > 570 || gei_flg == 2 || cnt > 200) {
            new HitsBomb(game, x, y, side);
            kill();
            return;
        }
        float targetRot = Math.round((float) Math.toDegrees(Math.atan2(game.core_y[1-side] - y, game.core_x[1-side] - x)));
        if (rot - 180 > targetRot) {
            targetRot += 360;
        }
        if (rot + 180 < targetRot) {
            targetRot -= 360;
        }
        if (speed < 15F) {
            speed += 0.3F;
        }
        if (speed > 14F) {
            rot = rot + (targetRot - rot) / 8F;
        }
        updateVelocity();
        move();
    }

    private void updateVelocity() {   //更新速度向量
        float rad = (float) Math.toRadians(rot);
        xs = (float) Math.cos(rad) * speed;
        ys = (float) Math.sin(rad) * speed;
    }

    @Override
    public void reflect(int from_rot){
        betray();
        rot = from_rot;
        updateVelocity();
    }
}
