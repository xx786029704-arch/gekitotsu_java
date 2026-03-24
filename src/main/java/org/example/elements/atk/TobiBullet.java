package org.example.elements.atk;

import org.example.Main;
import org.example.Utils;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;

public class TobiBullet extends Bullet {   //飞玉主弹
    private float rot;
    private float speed = 1F;
    private int hp = 3;
    private int cnt = 0;
    private int xl1 = 0;
    private int xl2 = 1920;

    public TobiBullet(float X, float Y, int S, int rotation) {   //初始化
        super(X, Y, S);
        rot = rotation;
        gei_flg = 0;
        if (side != 0) {
            xl2 = 2560;
        }
        else {
            xl1 = -640;
        }
        xs = Utils.cos(rotation) * speed;
        ys = Utils.sin(rotation) * speed;
    }

    @Override
    public void step() {   //每帧逻辑
        if (y < -600 || x > xl2 || x < xl1) {
            kill();
            return;
        }
        if (Main.team[1 - side].hitTestPoint(x, y)) {
            hp--;
        }
        if (hp <= 0 || y > 570) {
            new HitsBomb(x, y, side);
            kill();
            return;
        }
        float tRot = 180 * side;
        if (rot - 180 > tRot) {
            tRot = tRot + 360;
        }
        if (rot + 180 < tRot) {
            tRot = tRot - 360;
        }
        rot = rot + (tRot - rot) / 32;
        float rad = (float) Math.toRadians(rot);
        xs = (float) Math.cos(rad) * speed;
        ys = (float) Math.sin(rad) * speed;
        x = x + xs;
        y = y + ys;
        xySync();
        if (speed < 4F) {
            speed = speed + 0.5F;
        }
        cnt++;
        if (cnt == 20) {
            cnt = 0;
            new TobiBombBullet(x, y + 10, side);
        }
    }
}
