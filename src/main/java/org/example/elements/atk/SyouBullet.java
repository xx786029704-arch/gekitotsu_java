package org.example.elements.atk;

import org.example.CompositeShape;
import org.example.Main;
import org.example.ShapeBuilder;
import org.example.Utils;
import org.example.elements.hit.HitsDrop;

public class SyouBullet extends CompositeShape {   //障玉障壁
    private final int side;
    private final int id;
    private int hp = 10;
    private int cnt = 0;
    private float power = 20F;
    private final float cosRot;
    private final float sinRot;
    private boolean active = false;
    private int gei_flg = 1;

    public SyouBullet(float X, float Y, int S, int rotation) {   //初始化
        super(X, Y);
        ShapeBuilder.into(this)
                .rectangle(-14F, -14F, 28F, 28F);
        this.side = S;
        this.cosRot = Utils.cos(rotation);
        this.sinRot = Utils.sin(rotation);
        this.id = Main.addElement(this);
        moveToQuantized(X, Y);
    }

    @Override
    public void step() {   //每帧逻辑
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            this.hp--;
            if (Main.fort[1 - this.side].hitTestPoint(this.x, this.y) || Main.shield[1 - this.side].hitTestPoint(this.x, this.y)) {
                this.hp = 0;
            }
        }
        if (this.y > 570 || this.y < -600 || this.x > 1920 || this.x < 0 || this.hp <= 0 || this.cnt >= 160) {
            if (this.active) {
                new HitsDrop(this.x, this.y, Main.atk[this.side]);
            }
            kill();
            return;
        }
        boolean wasActive = this.active;
        if (this.power > 0) {
            this.power--;
            float newX = this.x + this.cosRot * this.power;
            float newY = this.y + this.sinRot * this.power;
            moveToQuantized(newX, newY);
            if (this.power == 0) {
                activate();
            }
        }
        if (wasActive) {
            this.cnt++;
        }
    }

    private void activate() {
        if (!this.active) {
            this.active = true;
            Main.unit[this.side].addShape(this);
        }
    }

    private void kill() {
        Main.elements.remove(id);
        if (this.active) {
            Main.unit[this.side].removeShape(this);
        }
    }

    public boolean pushBy(float dx, float dy) {
        float newX = this.x + dx;
        float newY = this.y + dy;
        moveToQuantized(newX, newY);
        if (this.y >= 566) {
            moveToQuantized(this.x, 566);
        }
        if (this.x < 0 || this.x > 1920) {
            kill();
            return true;
        }
        return false;
    }

    private void moveToQuantized(float newX, float newY) {
        float qx = (int) (20 * newX) * 0.05F;
        float qy = (int) (20 * newY) * 0.05F;
        moveTo(qx, qy);
    }
}
