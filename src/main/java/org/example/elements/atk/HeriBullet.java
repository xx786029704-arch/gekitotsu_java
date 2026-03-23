package org.example.elements.atk;

import org.example.Main;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;

public class HeriBullet extends Bullet {   //旋玉主弹
    private float rot;
    private float tRot;
    private float speed = 1F;
    private int hp = 3;
    private int cnt = 0;
    private int xl1 = 0;
    private int xl2 = 1920;
    private int yrot = 0;
    private float ysin = 0;
    private final int power = 30;

    public HeriBullet(float X, float Y, int S, int rotation) {   //初始化
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
        this.rot = this.rot + (this.tRot - this.rot) / 16;
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * this.speed;
        this.ys = (float) Math.sin(rad) * this.speed;
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        xySync();
        this.yrot = this.yrot + 5;
        if (this.yrot > 360) {
            this.yrot = this.yrot - 360;
        }
        this.ysin = (float) Math.sin(Math.toRadians(this.yrot)) * 3F;
        this.y = this.y + this.ysin;
        ySync();
        if (this.speed < 3F) {
            this.speed = this.speed + 0.4F;
        }
        this.cnt++;
        if (this.cnt == this.power || this.cnt == this.power + 6 || this.cnt == this.power + 12) {
            if (this.cnt == this.power + 12) {
                this.cnt = 0;
            }
            int scale = (this.rot < 90 || this.rot > 270) ? 1 : -1;
            int atkRot = Math.round(this.rot + 45 * scale);
            new GunBullet(this.x, this.y, this.side).setVecR(atkRot, 20).move();
        }
    }
}
