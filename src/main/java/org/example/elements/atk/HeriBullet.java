package org.example.elements.atk;

import org.example.Game;
import org.example.Utils;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;

public class HeriBullet extends Bullet {   //旋玉主弹
    private float rot;
    private float speed = 1F;
    private int hp = 3;
    private int cnt = 0;
    private int xl1 = 0;
    private int xl2 = 1920;
    private int yrot = 0;

    public HeriBullet(Game game, float X, float Y, int S, int rotation) {   //初始化
        super(game, X, Y, S);
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
        if (this.game.team[1 - side].hitTestPoint(x, y)) {
            hp--;
        }
        if (hp <= 0 || y > 570) {
            new HitsBomb(this.game, x, y, side);
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
        rot = rot + (tRot - rot) / 16;
        float rad = (float) Math.toRadians(rot);
        xs = (float) Math.cos(rad) * speed;
        ys = (float) Math.sin(rad) * speed;
        x = x + xs;
        y = y + ys;
        xySync();
        yrot = yrot + 5;
        if (yrot > 360) {
            yrot = yrot - 360;
        }
        y = y + Utils.sin(yrot) * 3F;
        ySync();
        if (speed < 3.01F) {
            speed = speed + 0.4F;
        }
        cnt++;
        int power = 30;
        if (cnt == power || cnt == power + 6 || cnt == power + 12) {
            if (cnt == power + 12) {
                cnt = 0;
            }
            int scale = (rot < 90 || rot > 270) ? 1 : -1;
            int atkRot = Math.round(rot + 45 * scale);
            new GunBullet(this.game, x, y, side).setVecR(atkRot, 20).move();
        }
    }
}
