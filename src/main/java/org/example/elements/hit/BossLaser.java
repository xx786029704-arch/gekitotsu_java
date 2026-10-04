package org.example.elements.hit;

import org.example.GameTask;
import org.example.Main;
import org.example.Utils;

public class BossLaser extends LaserBase {     //魔玉的激光

    public BossLaser(GameTask GAME, float X, float Y, int S) {
        super(GAME, X, Y, 90, S, 30.F, 15.5F);
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
        if (game.hp0_flg[this.side] > 0) {
            kill();
            return;
        }
        this.rotDeg -= this.side == 0 ? .1F : -.1F;
        this.rotDeg = ((this.rotDeg % 360) + 360) % 360;
        this.startX = game.cores[this.side].x + 38.f * internalCos(this.rotDeg);
        this.startY = game.cores[this.side].y + 38.f * internalSin(this.rotDeg);
    }
}