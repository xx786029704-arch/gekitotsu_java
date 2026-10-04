package org.example.elements.hit;

import org.example.GameTask;
import org.example.Shape;
import org.example.Utils;
import org.example.elements.units.MagicBall;

public class MagicLaser extends LaserBase {     //魔玉的激光

    private final int user;

    public MagicLaser(GameTask GAME, float X, float Y, int RDeg, int S, int U) {
        super(GAME, X, Y, RDeg, S, 20.F, 15.5F);
        user = U;
    }

    @Override
    protected float internalCos(float RDeg) {
        return Utils.cos((int) RDeg);
    }

    @Override
    protected float internalSin(float RDeg) {
        return Utils.sin((int) RDeg);
    }

    @Override
    protected void follow(){
        Shape s = game.elements.get(user);
        if (s instanceof MagicBall wrk) {
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
        }
    }
}