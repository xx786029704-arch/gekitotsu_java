package org.example.elements;

import org.example.*;
import org.example.Shape;
import org.example.elements.units.NieBall;

import java.awt.*;

public class Base extends CompositeShape {  //车板类
    public int axl;
    public int side;
    private float wrk;
    public float xs;
    public float ys;
    private float xx;
    private float yy;
    private float old_x;
    private float old_y;
    public float base_move_x = 0;
    public float base_move_y = 0;
    private final GameTask game;

    //TODO：车板可以像要塞壁一样优化碰撞，同时也不再需要子图形（最终版本）
    public Base(GameTask GAME, float X, float Y, int S) {
        super(X, Y);
        game = GAME;
        this.side = S;
        this.axl = 1;
        this.xx = x;
        this.yy = y;
        this.old_x = x;
        this.old_y = y;
        id = game.addElement(this);
        game.wall[side].addShape(this);
    }

    public void kill() {
        game.bases[side] = null;
        game.elements.remove(id);
    }

    @Override
    public void step(){     //照搬原版逻辑
        this.xs = this.xs + (float) this.axl / 1000;
        if (game.hp0_flg[this.side] >= 3) {
            this.xs = 0;
        }
        this.ys += 0.05F;
        this.xx += (this.xs - 2 * this.xs * this.side);
        this.yy += this.ys;
        if (this.yy > 532) {
            this.yy = 532;
            this.ys *= -0.5F;
        }
        this.moveTo(Math.round(this.xx),Math.round(this.yy));
        this.base_move_x = this.x - this.old_x;
        this.base_move_y = this.y - this.old_y;
        this.old_x = this.x;
        this.old_y = this.y;
        boolean tobasare_flg = false;
        if (this.x < 190) {
            this.wrk = Math.round((-this.xs) * 5) + 1;
            this.xx = 191;
            this.xs = (-this.xs) / 2;
            tobasare_flg = true;
        }
        else if (this.x > 1730) {
            this.wrk = Math.round((-this.xs) * 5) + 1;
            this.xx = 1729;
            this.xs = (-this.xs) / 2;
            tobasare_flg = true;
        }
        if (tobasare_flg && game.hp0_flg[this.side] == 0) {
            boolean nie_flg = false;
            for (Shape s : game.unit[side].getShapes()) {
                if (s instanceof NieBall nie) {
                    nie.alarm = 6;
                    nie_flg = true;
                    break;
                }
            }
            if (!nie_flg){
                game.dokkan_flg[this.side] = true;
                wrk = Math.round(wrk);
                if (this.wrk < 0) {
                    this.wrk = 0;
                }
                game.hp[this.side] -= (int) this.wrk;
                game.cores[side].dmg_flg = true;
            }
        }
    }

    @Override   //神之一手来了
    public boolean hitTestPoint(float X, float Y){
        float dx = X - x;
        float dy = Y - y;
        if (dx < -191.5F || dx > 191.5F || dy < -15.5F || dy > 51.5F){
            return false;
        }
        if (dy > -4F){
            if (dy > 17F){
                if (dy > 27.5F) {
                    dx = Math.abs(dx) - 108.7F;
                    dy -= 20;
                    return dx * dx + dy * dy <= 992.25F;
                }
                dy -= 17F;
                dy *= 1.095238095238F;
            } else {
                return true;
            }
        } else {
            dy += 4F;
        }
        if (dx > -180F){
            if (dx > 180F){
                dx -= 180F;
            } else {
                return true;
            }
        } else {
            dx += 180F;
        }
        return dx * dx + dy * dy <= 132.25F;
    }
}
