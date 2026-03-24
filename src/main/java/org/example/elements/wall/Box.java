package org.example.elements.wall;

import org.example.Main;
import org.example.elements.Wall;
import org.example.elements.hit.HitsDropFrames;

public class Box extends Wall {
    private int on_side;
    private float ys = 0;

    public Box(float X, float Y, int S, int TYPE, int ON_SIDE) {
        super(X, Y, S, TYPE);
        xySync();
        max_hp = 15;
        hp = 15;
        on_side = ON_SIDE;
    }

    @Override
    public void step(){
        if (on_side != -1) {
            if (Main.bases[side] == null) {
                kill();
                return;
            }
            move(Main.bases[side].base_move_x, Main.bases[side].base_move_y);
            ys = ys + 1;
            float drop_y = y + ys;
            if (drop_y >= 566) {
                drop_y = 566;
                on_side = -1;
            }
            else {
                y += 100;
                while (Main.fort[on_side].hitTestPoint(x, drop_y + 15)) {
                    drop_y -= 1;
                    ys = 0;
                }
                y -= 100;
            }
            moveTo(x, drop_y);
        }
        else if (Main.wall[1-side].hitTestPoint(x, y)) {
            hp = 0;
        }
        stepEx();
        if (Main.atk[1-side].hitTestPoint(x - 6, y - 6) ||
                Main.atk[1-side].hitTestPoint(x + 6, y - 6) ||
                Main.atk[1-side].hitTestPoint(x - 6, y + 6) ||
                Main.atk[1-side].hitTestPoint(x + 6, y + 6)) {
            hp--;
        }
        if (hp <= 0 || Main.hp0_flg[side] > 0 || y < -600) {
            new HitsDropFrames(this.x, this.y, Main.wall[side], 1, 1.3F);
            kill();
            return;
        }
        if (hp < max_hp && Main.repair[side].hitTestPoint(x, y)) {
            hp ++;
        }
    }
}
