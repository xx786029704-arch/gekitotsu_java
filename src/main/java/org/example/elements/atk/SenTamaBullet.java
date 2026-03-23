package org.example.elements.atk;

import org.example.Main;
import org.example.Utils;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBombMult;

public class SenTamaBullet extends Bullet {   //战玉子弹
    private int hp = 3;
    private final float power;

    public SenTamaBullet(float X, float Y, int S, int rotation, float power) {   //初始化
        super(X, Y, S);
        this.power = power;
        this.xs = Utils.cos(rotation) * this.power;
        this.ys = Utils.sin(rotation) * this.power;
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y > 570) {
            this.hp = 0;
        }
        else if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            if (Main.fort[1 - this.side].hitTestPoint(this.x, this.y) || Main.shield[1 - this.side].hitTestPoint(this.x, this.y)) {
                this.hp = 0;
            }
            else {
                this.hp--;
            }
        }
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        this.xySync();
        this.ys = this.ys + 0.32F;
        if (this.hp <= 0) {
            new HitsBombMult(this.x, this.y, this.side, 0.5F);
            kill();
        }
    }
}
