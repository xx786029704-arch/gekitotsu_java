package org.example.elements.atk;

import org.example.Game;
import org.example.Shape;
import org.example.elements.Ball;
import org.example.elements.Bullet;
import org.example.elements.units.GeiBall;

import java.awt.*;

public class HolyBullet extends Shape {
    private int cnt = 0;
    private final int t_type;
    private final int t_rot;
    private final int t_side;
    private final int t_on_side;
    private final int t_jump_flg;
    protected final Game game;


    public HolyBullet(Game game, float X, float Y, int TYPE, int ROT, int SIDE, int ON_SIDE, int JUMP_FLG) {
        super(X, Y);
        this.game=game;
        t_type = TYPE;
        t_rot = ROT;
        t_on_side = ON_SIDE;
        t_side = SIDE;
        t_jump_flg = JUMP_FLG;
        id = this.game.addElement(this);
    }

    public void kill() {
        this.game.elements.remove(id);
    }

    @Override
    public void step(){
        if (t_jump_flg == 0 && this.game.bases[t_on_side] != null) {
            x = x + this.game.bases[t_on_side].base_move_x;
            y = y + this.game.bases[t_on_side].base_move_y;
        }
        cnt++;
        if (cnt == 20) {
            //TODO:需要处理复活玉的随机数问题
            this.game.unit_make(x, y, t_rot, t_type, t_side);
            ((Ball) this.game.elements.get(this.game.ID - 1)).on_side = t_on_side;
        } else if(cnt >= 30){
            kill();
        }
    }

    @Override
    public boolean hitTestPoint(float X, float Y) {
        return false;
    }

    @Override
    public void draw(Graphics2D g2d) {
    }
}
