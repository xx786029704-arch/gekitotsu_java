package org.example.elements;

import org.example.Main;
import org.example.elements.hit.HitsDrop;

public class NinBullet extends Bullet {   //忍玉手里剑
    private int bounceCount = 0;

    public NinBullet(float X, float Y, int S, float rotation) {   //初始化
        super(X, Y, S);
        this.rot = rotation;
        this.gei_flg = 1;
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * 18F;
        this.ys = (float) Math.sin(rad) * 18F;
        this.x = X + this.xs;
        this.y = Y + this.ys;
        this.r = 12.4F;
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.x > 1920) {
            if (this.side != 1) {
                this.bounceCount = 99;
            }
            else {
                this.x = 0;
                this.bounceCount++;
            }
        }
        else if (this.x < 0) {
            if (this.side != 0) {
                this.bounceCount = 99;
            }
            else {
                this.x = 1920;
                this.bounceCount++;
            }
        }
        if (this.bounceCount > 1) {
            kill();
            return;
        }
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            new HitsDrop(this.x, this.y, Main.atk[this.side]);
            kill();
            return;
        }
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        if (this.xs < 0) {
            this.rot = this.rot - 12;
        }
        else {
            this.rot = this.rot + 12;
        }
    }
}
