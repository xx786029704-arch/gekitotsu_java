package org.example.elements;

import org.example.CompositeShape;
import org.example.Main;
import org.example.Shape;
import org.example.ShapeBuilder;

import java.awt.*;

public class base extends CompositeShape {
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
    float base_move_x;
    float base_move_y;

    public base(float X, float Y, int S) {
        super(X, Y);
        this.side = S;
        this.axl = 1;
        this.xx = x;
        this.yy = y;
        ShapeBuilder.into(this)
                .roundedRectangle(-190,-14,380,40,10)
                .circle(-108.7F,20,30)
                .circle(108.7F,20,30);
        Main.elements.add(this);
        Main.wall[side].addShape(this);
    }

    @Override
    public void step(){
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
            //Main.core[side].dmg_flg = true;
        }
    }
}
