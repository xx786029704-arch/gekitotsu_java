package org.example.elements.hit;

import org.example.Main;
import org.example.elements.hit.LaserBase;
import org.example.Utils;
import org.example.elements.units.MagicBall;

public class MagicLaser extends LaserBase {     //魔玉的激光

    private final int user;

    public MagicLaser(float X, float Y, int RDeg, int S,int U) {
        super(X, Y, RDeg, S, 20.F, 15.5F);
        user = U;
    }

    @Override
    protected float internalCos(float RDeg) {
        return Utils.cos((int)RDeg);
    }

    @Override
    protected float internalSin(float RDeg) {
        return Utils.sin((int)RDeg);
    }

    @Override
    protected void follow(){
        if (Main.elements.containsKey(user)) {
            //此处代码缺乏安全性，但一般应该能保证user一定是MagicBall
            MagicBall wrk = (MagicBall) (Main.elements.get(user));
            if (!wrk.shooting) {
                kill();
                return;
            }
            this.rotDeg=wrk.rot;
            this.startX = wrk.x + 38.f * internalCos(this.rotDeg);
            this.startY = wrk.y + 38.f * internalSin(this.rotDeg);
        }
        else{
            kill();
            return;
        }
    }

}