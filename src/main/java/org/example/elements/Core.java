package org.example.elements;

import org.example.CompositeShape;
import org.example.GameTask;
import org.example.ShapeBuilder;

import java.awt.*;

public class Core extends CompositeShape {      //核心类
    public int side;
    public float unit_x;
    public float unit_y;
    protected final GameTask game;

    public Core(GameTask GAME, float X, float Y, int S) {
        super(X, Y);
        game = GAME;
        this.side = S;
        unit_x = X;
        unit_y = Y;
        id = game.addElement(this);
        game.wall[side].addShape(this);
    }

    public void kill() {
        game.cores[side] = null;
        game.elements.remove(id);
        game.wall[side].removeShape(this);
    }

    @Override
    public void step(){     //照搬原版逻辑
        this.move(game.bases[side].x + this.unit_x - x,game.bases[side].y + this.unit_y - y);
        if (game.atk[1-side].hitTestPoint(this.x - 40, this.y - 40) ||
            game.atk[1-side].hitTestPoint(this.x + 40, this.y - 40) ||
            game.atk[1-side].hitTestPoint(this.x - 40, this.y + 40) ||
            game.atk[1-side].hitTestPoint(this.x + 40, this.y + 40) ||
            game.atk[1-side].hitTestPoint(this.x - 20, this.y - 20) ||
            game.atk[1-side].hitTestPoint(this.x, this.y - 20) ||
            game.atk[1-side].hitTestPoint(this.x + 20, this.y - 20) ||
            game.atk[1-side].hitTestPoint(this.x - 20, this.y + 20) ||
            game.atk[1-side].hitTestPoint(this.x, this.y + 20) ||
            game.atk[1-side].hitTestPoint(this.x + 20, this.y + 20) ||
            game.atk[1-side].hitTestPoint(this.x - 20, this.y) ||
            game.atk[1-side].hitTestPoint(this.x + 20, this.y) ||
            game.atk[1-side].hitTestPoint(this.x, this.y)) {
            game.hp[side]--;
            }
        if (game.hp[side] < 100 && game.repair[side].hitTestPoint(this.x, this.y)) {
            game.hp[side]++;
        }
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
