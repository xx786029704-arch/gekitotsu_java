package org.example.elements.wall;

import org.example.GameTask;
import org.example.elements.Wall;
import org.example.elements.hit.HitsDropFrames;

public class Box extends Wall {
    private int on_side;
    private float ys = 0;

    public Box(GameTask GAME, float X, float Y, int S, int TYPE, int ON_SIDE) {
        super(GAME, X, Y, S, TYPE);
        xySync();
        max_hp = 15;
        hp = 15;
        on_side = ON_SIDE;
    }

    @Override
    public void step(){
        if (on_side != -1) {
            if (game.bases[side] == null) {
                kill();
                return;
            }
            move(game.bases[side].base_move_x, game.bases[side].base_move_y);
            ys = ys + 1;
            float drop_y = y + ys;
            if (drop_y >= 566) {
                drop_y = 566;
                on_side = -1;
            }
            else {
                y += 100;
                while (game.fort[on_side].hitTestPoint(x, drop_y + 15)) {
                    drop_y -= 1;
                    ys = 0;
                }
                y -= 100;
            }
            moveTo(x, drop_y);
        }
        else if (game.wall[1-side].hitTestPoint(x, y)) {
            hp = 0;
        }
        stepEx();
        if (game.atk[1-side].hitTestPoint(x - 6, y - 6) ||
                game.atk[1-side].hitTestPoint(x + 6, y - 6) ||
                game.atk[1-side].hitTestPoint(x - 6, y + 6) ||
                game.atk[1-side].hitTestPoint(x + 6, y + 6)) {
            hp--;
        }
        if (hp <= 0 || game.hp0_flg[side] > 0 || y < -600) {
            new HitsDropFrames(game, x, y, game.wall[side], 1, 1.3F);
            kill();
            return;
        }
        if (hp < max_hp && game.repair[side].hitTestPoint(x, y)) {
            hp ++;
        }
    }
}
