package org.example.elements.hit;

import org.example.Main;
import org.example.Utils;

public class BossLaser extends LaserBase {     //魔玉的激光

    public BossLaser(float X, float Y, int S) {
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
        this.rotDeg -= this.side==0?.1F:-.1F;
        this.rotDeg=((this.rotDeg%360)+360)%360;
        this.startX = Main.core_x[this.side] + 38.f * internalCos(this.rotDeg);
        this.startY = Main.core_y[this.side] + 38.f * internalSin(this.rotDeg);
    }
}
