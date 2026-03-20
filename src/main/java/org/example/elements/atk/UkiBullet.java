package org.example.elements.atk;

import org.example.Main;
import org.example.Round;
import org.example.elements.hit.HitsBombMult;

public class UkiBullet extends Round {   //浮玉子弹
    private final int side;
    private final int id;
    private int gei_flg = 0;
    private int hp = 1;
    private float rot = 0;
    private float xs = 0;
    private float ys = 0;
    private float yrot = 0;
    private float yrad = 0;
    private float ysin = 0;
    private float ycos = 0;

    public UkiBullet(float X, float Y, int S, float rotation) {   //初始化
        super(X, Y, 15.5F);
        this.side = S;
        this.rot = rotation;
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * 3F;
        this.ys = (float) Math.sin(rad) * 3F;
        this.id = Main.addElement(this);
        Main.unit[side].addShape(this);
    }

    @Override
    public void step() {   //每帧逻辑
        updateFloatOffset();
        if (this.y > 570) {
            this.hp = 0;
        }
        else if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            if (Main.wall[1 - this.side].hitTestPoint(this.x, this.y) || Main.shield[1 - this.side].hitTestPoint(this.x, this.y)) {
                this.hp = 0;
            }
            else {
                this.hp--;
            }
        }
        this.rot = this.rot + this.xs;
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        this.x = this.x + this.ycos;
        this.y = this.y + this.ysin;
        if (this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (this.hp <= 0) {
            new HitsBombMult(this.x, this.y, this.side, 1.2F);
            kill();
        }
    }

    public void kill() {
        Main.elements.remove(id);
        Main.unit[this.side].removeShape(this);
    }

    private void updateFloatOffset() {
        this.yrot = this.yrot + 10F;
        if (this.yrot > 360F) {
            this.yrot = this.yrot - 360F;
        }
        this.yrad = (float) Math.toRadians(this.yrot);
        this.ysin = (float) Math.sin(this.yrad);
        this.ycos = (float) Math.cos(this.yrad);
    }
}
