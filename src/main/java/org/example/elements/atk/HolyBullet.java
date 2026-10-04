package org.example.elements.atk;

import org.example.GameTask;
import org.example.Shape;
import org.example.elements.Ball;

import java.awt.*;

public class HolyBullet extends Shape {
    private int cnt = 0;
    private final int t_type;
    private final int t_rot;
    private final int t_side;
    private final int t_on_side;
    private final int t_jump_flg;
    private final GameTask game;


    public HolyBullet(GameTask GAME, float X, float Y, int TYPE, int ROT, int SIDE, int ON_SIDE, int JUMP_FLG) {
        super(X, Y);
        xySync();
        game = GAME;
        t_type = TYPE;
        t_rot = ROT;
        t_on_side = ON_SIDE;
        t_side = SIDE;
        t_jump_flg = JUMP_FLG;
        id = game.addElement(this);
    }

    public void kill() {
        game.elements.remove(id);
    }

    @Override
    public void step(){
        if (t_jump_flg == 0 && game.bases[t_on_side] != null) {
            x = x + game.bases[t_on_side].base_move_x;
            y = y + game.bases[t_on_side].base_move_y;
        }
        cnt++;
        if (cnt == 20) {
            //TODO:需要处理复活玉的随机数问题
            game.unit_make(game, x, y, t_rot, t_type, t_side);
            ((Ball) game.elements.get(game.ID - 1)).on_side = t_on_side;
        } else if(cnt >= 30){
            kill();
        }
    }

    @Override
    public boolean hitTestPoint(float X, float Y) {
        return false;
    }
}
