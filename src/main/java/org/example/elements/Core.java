package org.example.elements;

import org.example.CompositeShape;
import org.example.Main;
import org.example.ShapeBuilder;

import java.awt.*;

public class Core extends CompositeShape {      //核心类
    public int side;
    public float unit_x;
    public float unit_y;
    public boolean dmg_flg;

    public Core(float X, float Y, int S) {
        super(X, Y);
        this.side = S;
        unit_x = X;
        unit_y = Y;
        dmg_flg = false;
        float[][] vxy1 = {{-27.63934F,-36.86066F},{-36.86066F,-27.63934F},{27.63934F,36.86066F},{36.86066F,27.63934F}};
        float[][] vxy2 = {{-27.63934F,36.86066F},{-36.86066F,27.63934F},{27.63934F,-36.86066F},{36.86066F,-27.63934F}};
        ShapeBuilder.into(this)     //5个圆2个多边形
                .circle(-40,-40,13.5F)
                .circle(-40,40,13.5F)
                .circle(40,40,13.5F)
                .circle(40,-40,13.5F)
                .circle(0,-0,33.5F)
                .polygon(0,0,vxy1)
                .polygon(0,0,vxy2);
        id = Main.addElement(this);
        Main.wall[side].addShape(this);
    }

    public void kill() {
        Main.cores[side] = null;
        Main.elements.remove(id);
        Main.wall[side].removeShape(this);
    }

    @Override
    public void step(){     //照搬原版逻辑
        this.move(Main.bases[side].x + this.unit_x - x,Main.bases[side].y + this.unit_y - y);
        if (Main.atk[1-side].hitTestPoint(this.x - 40, this.y - 40) ||
            Main.atk[1-side].hitTestPoint(this.x + 40, this.y - 40) ||
            Main.atk[1-side].hitTestPoint(this.x - 40, this.y + 40) ||
            Main.atk[1-side].hitTestPoint(this.x + 40, this.y + 40) ||
            Main.atk[1-side].hitTestPoint(this.x - 20, this.y - 20) ||
            Main.atk[1-side].hitTestPoint(this.x, this.y - 20) ||
            Main.atk[1-side].hitTestPoint(this.x + 20, this.y - 20) ||
            Main.atk[1-side].hitTestPoint(this.x - 20, this.y + 20) ||
            Main.atk[1-side].hitTestPoint(this.x, this.y + 20) ||
            Main.atk[1-side].hitTestPoint(this.x + 20, this.y + 20) ||
            Main.atk[1-side].hitTestPoint(this.x - 20, this.y) ||
            Main.atk[1-side].hitTestPoint(this.x + 20, this.y) ||
            Main.atk[1-side].hitTestPoint(this.x, this.y)) {
            this.dmg_flg = true;
            Main.hp[side]--;
            }
        if (Main.hp[side] < 100 && Main.repair[side].hitTestPoint(this.x, this.y)) {
            Main.hp[side]++;
        }
        dmg_flg = false;
    }

    @Override   //神之一手来了
    public boolean hitTestPoint(float X, float Y){
        float dx = X - x;
        float dy = Y - y;
        if (dx < -53.5F || dx > 53.5F || dy < -53.5F || dy > 53.5F){
            return false;
        }
        dx = Math.abs(dx);
        dy = Math.abs(dy);
        if(dy - dx <= 9.221316F && dy - dx >= -9.221316F && dx < 40) return true;
        if(dx * dx + dy * dy <= 1122.25F) return true;
        dx -= 40;
        dy -= 40;
        return dx * dx + dy * dy <= 182.25F;
    }
}
