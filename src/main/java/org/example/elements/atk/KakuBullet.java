package org.example.elements.atk;

import org.example.Main;
import org.example.Round;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;
import org.example.elements.hit.HitsBombMult;

public class KakuBullet extends Bullet {   //核玉核弹
    private float speed;
    private int hp = 4;
    public float cos_rot;
    public float sin_rot;

    public KakuBullet(float X, float Y, int S, float cos_rot, float sin_rot) {   //初始化
        super(X, Y, S);
        this.speed = 1;
        this.cos_rot = cos_rot;
        this.sin_rot = sin_rot;
        this.xs = cos_rot;
        this.ys = sin_rot;
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y < -1200 || this.x > 2560 || this.x < -640) {
            kill();
            return;
        }
        if (Main.atk[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            this.hp--;
        }
        if (this.hp <= 0 || this.y > 570) {
            new HitsBomb(this.x, this.y, this.side);
            kill();
            return;
        }
        if (Main.wall[1 - this.side].hitTestPoint(this.x, this.y) || Main.shield[1 - this.side].hitTestPoint(this.x, this.y)) {
            new HitsBombMult(this.x, this.y, this.side, 8);
            kill();
            return;
        }
        if (this.speed < 3F) {
            this.speed = this.speed + 0.01F;
        }
        this.xs = cos_rot * this.speed;
        this.ys = sin_rot * this.speed;
        move();
    }
}
