package org.example.elements.atk;

import org.example.Main;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;

public class TobiBullet extends Bullet {   //飞玉主弹
    private float rot;
    private float tRot;
    private float speed = 1F;
    private int hp = 3;
    private int cnt = 0;
    private int xl1 = 0;
    private int xl2 = 1920;

    public TobiBullet(float X, float Y, int S, int rotation) {   //初始化
        super(X, Y, S);
        this.rot = rotation;
        this.gei_flg = 0;
        if (this.side != 0) {
            this.xl2 = 2560;
        }
        else {
            this.xl1 = -640;
        }
        float rad = (float) Math.toRadians(rotation);
        this.xs = (float) Math.cos(rad) * this.speed;
        this.ys = (float) Math.sin(rad) * this.speed;
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y < -600 || this.x > this.xl2 || this.x < this.xl1) {
            kill();
            return;
        }
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y)) {
            this.hp--;
        }
        if (this.hp <= 0 || this.y > 570) {
            new HitsBomb(this.x, this.y, this.side);
            kill();
            return;
        }
        this.tRot = 180 * this.side;
        if (this.rot - 180 > this.tRot) {
            this.tRot = this.tRot + 360;
        }
        if (this.rot + 180 < this.tRot) {
            this.tRot = this.tRot - 360;
        }
        this.rot = this.rot + (this.tRot - this.rot) / 32;
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * this.speed;
        this.ys = (float) Math.sin(rad) * this.speed;
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        xySync();
        if (this.speed < 4F) {
            this.speed = this.speed + 0.5F;
        }
        this.cnt++;
        if (this.cnt == 20) {
            this.cnt = 0;
            new TobiBombBullet(this.x, this.y + 10, this.side);
        }
    }
}
