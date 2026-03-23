package org.example.elements.atk;

import org.example.Main;
import org.example.Utils;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBombMult;

public class TuiMissileBullet extends Bullet {   //坠玉导弹
    private float speed = 15F;
    private int rot;

    public TuiMissileBullet(float X, float Y, int S, int rotation) {   //初始化
        super(X, Y, S);
        this.rot = rotation;
        this.xs = Utils.cos(rotation) * this.speed;
        this.ys = Utils.sin(rotation) * this.speed;
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y < -1200 || this.x > 2560 || this.x < -640) {
            kill();
            return;
        }
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.y > 570 || this.gei_flg == 2) {
            new HitsBombMult(this.x, this.y, this.side, 0.5F);
            kill();
            return;
        }
        if (this.speed < 20F) {
            this.speed = this.speed + 1F;
        }
        this.xs = Utils.cos(this.rot) * this.speed;
        this.ys = Utils.sin(this.rot) * this.speed;
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        xySync();
    }
}
