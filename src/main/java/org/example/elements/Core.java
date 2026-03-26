package org.example.elements;

import org.example.CompositeShape;
import org.example.Game;
import org.example.ShapeBuilder;

import java.awt.*;

public class Core extends CompositeShape {      //核心类
    public int side;
    public float unit_x;
    public float unit_y;
    public boolean dmg_flg;
    protected final Game game;

    public Core(Game game, float X, float Y, int S) {
        super(X, Y);
        this.game=game;
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
        id = this.game.addElement(this);
        this.game.wall[side].addShape(this);
    }

    public void kill() {
        this.game.cores[side] = null;
        this.game.elements.remove(id);
        this.game.wall[side].removeShape(this);
    }

    @Override
    public void step(){     //照搬原版逻辑
        this.move(this.game.bases[side].x + this.unit_x - x,this.game.bases[side].y + this.unit_y - y);
        if (this.game.atk[1-side].hitTestPoint(this.x - 40, this.y - 40) ||
            this.game.atk[1-side].hitTestPoint(this.x + 40, this.y - 40) ||
            this.game.atk[1-side].hitTestPoint(this.x - 40, this.y + 40) ||
            this.game.atk[1-side].hitTestPoint(this.x + 40, this.y + 40) ||
            this.game.atk[1-side].hitTestPoint(this.x - 20, this.y - 20) ||
            this.game.atk[1-side].hitTestPoint(this.x, this.y - 20) ||
            this.game.atk[1-side].hitTestPoint(this.x + 20, this.y - 20) ||
            this.game.atk[1-side].hitTestPoint(this.x - 20, this.y + 20) ||
            this.game.atk[1-side].hitTestPoint(this.x, this.y + 20) ||
            this.game.atk[1-side].hitTestPoint(this.x + 20, this.y + 20) ||
            this.game.atk[1-side].hitTestPoint(this.x - 20, this.y) ||
            this.game.atk[1-side].hitTestPoint(this.x + 20, this.y) ||
            this.game.atk[1-side].hitTestPoint(this.x, this.y)) {
            this.dmg_flg = true;
            this.game.hp[side]--;
            }
        if (this.game.hp[side] < 100 && this.game.repair[side].hitTestPoint(this.x, this.y)) {
            this.game.hp[side]++;
        }
        dmg_flg = false;
    }
}
