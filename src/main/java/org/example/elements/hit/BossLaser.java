package org.example.elements.hit;

import org.example.Game;
import org.example.Utils;

public class BossLaser extends LaserBase {     //魔玉的激光

    public BossLaser(Game game, float X, float Y, int S) {
        super(game, X, Y, 90, S, 30.F, 15.5F);
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
        if (this.game.hp0_flg[this.side] > 0) {
            kill();
            return;
        }
        this.rotDeg -= this.side == 0 ? .1F : -.1F;
        this.rotDeg = ((this.rotDeg % 360) + 360) % 360;
        this.startX = this.game.cores[this.side].x + 38.f * internalCos(this.rotDeg);
        this.startY = this.game.cores[this.side].y + 38.f * internalSin(this.rotDeg);
    }
}