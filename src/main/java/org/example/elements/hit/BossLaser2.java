package org.example.elements.hit;

import org.example.Main;

import java.awt.*;
import java.awt.geom.Path2D;

public class BossLaser2 extends LaserBase {     //魔玉的激光

    public BossLaser2(float X, float Y, int S) {
        super(X, Y, 90, S, 30.F, 15.5F);
    }

    @Override
    protected float internalCos(float RDeg) {
        return (float) Math.cos((double) RDeg * 0.017453292519943295F);
    }

    @Override
    protected float internalSin(float RDeg) {
        return (float) Math.sin((double) RDeg * 0.017453292519943295F);
    }

    @Override
    protected void follow() {
        if (Main.hp0_flg[this.side] > 0) {
            kill();
            return;
        }
        this.rotDeg -= this.side == 0 ? .3F : -.3F;
        this.rotDeg = ((this.rotDeg % 360) + 360) % 360;
        this.startX = Main.cores[this.side].x + 38.f * internalCos(this.rotDeg);
        this.startY = Main.cores[this.side].y + 38.f * internalSin(this.rotDeg);
    }

    @Override
    public boolean hitTestPoint(float X, float Y) {
        float dx = X - this.startX, dy = Y - this.startY;
        float wrk = dx * this.xSpeed + dy * this.ySpeed;
        if (wrk >= 0.f && wrk <= steps * 100.f) {
            float distx10 = Math.abs(dy * this.xSpeed - dx * this.ySpeed);
            if (distx10 <= 175.f) {
                if (distx10 > 29.f && distx10 < 58.5f || distx10 > 116.5f && distx10 < 146.f) return false;
                else return true;
            }
        }
        dx = X - this.x;
        dy = Y - this.y;
        return dx * dx + dy * dy <= this.circleRadius * this.circleRadius;
    }

}