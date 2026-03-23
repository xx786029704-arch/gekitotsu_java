package org.example.elements.atk;

import org.example.Main;
import org.example.Utils;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;

public class SenBullet extends Bullet {   //战玉坦克
    private float rot;
    private float tRot;
    private int hp = 3;
    private final float speed = 6F;
    private int cnt = 0;
    private boolean drop_flg = false;
    private int aRot = 0;

    public SenBullet(float X, float Y, int S, int rotation) {   //初始化
        super(X, Y, S);
        this.rot = rotation;
        this.gei_flg = 0;
        this.xs = Utils.cos(rotation) * this.speed;
        this.ys = Utils.sin(rotation) * this.speed;
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y < -1200 || this.x > 2560 || this.x < -640) {
            kill();
            return;
        }
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y)) {
            this.hp--;
        }
        if (this.hp <= 0) {
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
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        xySync();
        this.ys = this.ys + 0.32F;
        if (this.y > 566) {
            if (!this.drop_flg) {
                this.drop_flg = true;
            }
            this.y = 566;
            this.ys = (-this.ys) / 8F;
            this.xs = 2 - 4 * this.side;
        }
        if (this.drop_flg) {
            this.cnt++;
        }
        if (this.drop_flg && this.cnt == 40) {
            this.cnt = 0;
            this.aRot = -45 - 90 * this.side;
            float spawnX = this.x + Utils.cos(this.aRot) * 20;
            float spawnY = this.y + Utils.sin(this.aRot) * 20;
            new SenTamaBullet(spawnX, spawnY, this.side, this.aRot, 20F);
        }
    }
}
