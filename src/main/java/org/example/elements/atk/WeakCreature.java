package org.example.elements.atk;

import org.example.Main;
import org.example.Round;
import org.example.Utils;
import org.example.elements.hit.HitsDropFrames;
import org.example.elements.hit.HitsDrop;

public class WeakCreature extends Round {   //，，弱，，生物
    private final int side;
    private int hp = 3;
    private float xs = 0;
    private float ys = 0;
    private float xs2 = 0;
    private float rot = 0;

    public WeakCreature(float X, float Y, int S, int R) {   //初始化
        super(X, Y, 15.5F);
        xySync();
        this.side = S;
        this.rot = R;
        this.xs = Utils.cos(R) * 3.F;
        this.ys = Utils.sin(R) * 3.F;
        this.id = Main.addElement(this);
        Main.unit[side].addShape(this);
        Main.atk[side].addShape(this);
    }

    @Override
    public void step() {   //每帧逻辑
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y)) {
            this.xs2 = -4 + 8 * this.side;
            this.hp--;
            new HitsDropFrames(this.x, this.y, Main.atk[side], 1, 1.2F);
        }
        int targetRot = Math.round((float) Math.toDegrees(Math.atan2(Main.core_y[1 - this.side] - this.y, Main.core_x[1 - this.side] - this.x)));
        targetRot = (targetRot % 360 + 360) % 360;
        this.rot = this.rot * .875F + targetRot * .125F;
        this.xs = Utils.cosF(this.rot) * 3.F;
        this.ys = Utils.sinF(this.rot) * 3.F;
        this.x = this.x + this.xs + this.xs2;
        this.y = this.y + this.ys;
        xySync();
        this.xs2 *= .9F;
        if (this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (this.y > 570) {
            this.y = 570;
            this.ys = -this.ys;
        }
        if (this.hp <= 0) {
            new HitsDrop(this.x, this.y, Main.atk[side]);
            kill();
        }
    }

    public void kill() {
        Main.elements.remove(id);
        Main.unit[this.side].removeShape(this);
        Main.atk[this.side].removeShape(this);
    }

}