package org.example.elements;

import org.example.CompositeShape;
import org.example.Main;
import org.example.ShapeBuilder;

public class Base extends CompositeShape {  //车板类
    public int axl;
    public int side;
    boolean tobasare_flg = false;
    boolean wrk_flg = false;
    float wrk;
    public float xs;
    public float ys;
    float xx;
    float yy;
    float old_x;
    float old_y;
    float base_move_x = 0;
    float base_move_y = 0;

    public Base(float X, float Y, int S) {
        super(X, Y);
        this.side = S;
        this.axl = 1;
        this.xx = x;
        this.yy = y;
        this.old_x = x;
        this.old_y = y;
        ShapeBuilder.into(this)     //一个圆角矩形两个圆形
                .roundedRectangle(-191.5F,-15.5F,383,43,11.5F)
                .circle(-108.7F,20,31.5F)
                .circle(108.7F,20,31.5F);
        id = Main.addElement(this);
        Main.wall[side].addShape(this);
    }

    public void kill() {
        Main.bases[side] = null;
        Main.elements.remove(id);
    }

    @Override
    public void step(){     //照搬原版逻辑
        this.xs = this.xs + (float) this.axl / 1000;
        if (Main.hp0_flg[this.side] >= 3) {
            this.xs = 0;
        }
        this.ys += 0.05F;
        this.xx += (this.xs - 2 * this.xs * this.side);
        this.yy += this.ys;
        if (this.yy > 532) {
            this.yy = 532;
            this.ys *= -0.5F;
        }
        this.move(Math.round(this.xx) - x,Math.round(this.yy) - y);
        this.base_move_x = this.x - this.old_x;
        this.base_move_y = this.y - this.old_y;
        this.old_x = this.x;
        this.old_y = this.y;
        this.tobasare_flg = false;
        if (this.x < 190) {
            this.wrk = Math.round((-this.xs) * 5) + 1;
            this.xx = 191;
            this.xs = (-this.xs) / 2;
            this.tobasare_flg = true;
        }
        else if (this.x > 1730) {
            this.wrk = Math.round((-this.xs) * 5) + 1;
            this.xx = 1731;
            this.xs = (-this.xs) / 2;
            this.tobasare_flg = true;
        }
        if (this.tobasare_flg && Main.hp0_flg[this.side] == 0) {
            this.wrk_flg = true;
            Main.dokkan_flg[this.side] = true;
            wrk = Math.round(wrk);
            if (this.wrk < 0)
            {
                this.wrk = 0;
            }
            Main.hp[this.side] -= (int) - this.wrk;
            System.out.println("hit wall");
            Main.cores[side].dmg_flg = true;
        }
    }
}
