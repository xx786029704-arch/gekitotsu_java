package org.example.elements;

import org.example.Game;
import org.example.Round;
import org.example.Utils;
import org.example.elements.hit.HitsDrop;

public class Ball extends Round {       //兵玉基类
    public int side;
    public int type;
    public int cnt = 0;
    public int jump_flg = 0;
    public int speed = 0;
    public int on_side;
    public int hp = 10;
    public int max_hp = 10;
    public int rot;
    public float drop_y = 0;
    public float xs = 0;
    public float ys = 0;
    public float cos_rot;
    public float sin_rot;
    public Game game;
    //TODO：因为玉的角度都是整数，所以可以尝试做一个0-359度的正余弦值表



    public Ball(Game game, float X, float Y, int R, int S, int TYPE) {
        super(X, Y, 23.25F);
        xySync();
        this.game=game;
        this.side = S;
        this.on_side = S;
        this.rot = R;
        this.type = TYPE;
        this.cos_rot = Utils.cos(R);
        this.sin_rot = Utils.sin(R);
        id = this.game.addElement(this);
        this.game.unit[side].addShape(this);
    }

    public void kill() {
        this.game.elements.remove(id);
        this.game.unit[this.side].removeShape(this);
    }

    @Override
    public void step(){     //照搬原版unit_func()
        if (jump_flg != 1) {
            if (jump_flg != 2) {
                if (!land()) return;
            }
            else {
                if (!ground()) return;
            }
        }
        else {
            if (!jump()) return;
        }
        if (this.game.saihai_cnt[side] > 0) {
            if (jump_flg == 0 && side != on_side || jump_flg == 2) {
                jump_flg = 1;
                xs = Utils.cos(this.game.saihai_rot[side]) * 4;
                ys = -2 + Utils.sin(this.game.saihai_rot[side]) * 4;
                if (ys > -2) {
                    ys = -2;
                }
            }
        }
        if (jump_flg == 0 && this.game.jump_u[side].hitTestPoint(x, y)){
            jump_flg = 1;
            xs = 4 - 8 * on_side;
            ys = -8;
            on_side = 1 - on_side;
        }
        else if(jump_flg == 0 && this.game.jump_f[side].hitTestPoint(x, y)){
            jump_flg = 1;
            xs = 15 - 30 * on_side;
            ys = -4;
            on_side = 1 - on_side;
        }
        else if(this.game.snipe[side].hitTestPoint(x, y + 16)){
            snipe();
        }
        else if(this.game.turn_ccw[side].hitTestPoint(x, y + 16)){
            turn(-2);
        }
        else if(this.game.turn_cw[side].hitTestPoint(x, y + 16)){
            turn(2);
        }
        if (this.game.atk[1 - side].hitTestPoint(x - 8, y - 8) || this.game.atk[1 - side].hitTestPoint(x + 8, y - 8) || this.game.atk[1 - side].hitTestPoint(x - 8, y + 8) || this.game.atk[1 - side].hitTestPoint(x + 8, y + 8) || jump_flg == 0 && this.game.dokkan_flg[on_side]) {
            hurt(this.game.dokkan_flg[on_side]);     //从又臭又长的switch()改为调用自己的hurt方法
            hp--;
        }
        if (hp <= 0 || this.game.hp0_flg[on_side] > 0 && jump_flg == 0) {
            new HitsDrop(this.game, this.x, this.y, this.game.unit[side]);
            this.game.dead_last[side] = type;
            kill();
            return;
        }

        if (hp < max_hp) {
            if (this.game.heal[side].hitTestPoint(x, y)) {
                hp++;
            }
        }
        stepEx();
    }

    public void stepEx(){   //如果基础行动判定通过则进行的额外行为
    }

    public boolean land(){      //正常
        if (jump_flg == 0 && !(side == on_side) && this.game.hp0_flg[on_side] > 0) {
            jump_flg = 1;
            xs = Utils.random(this.game.seeder[this.side]) * 7 - 3;
            ys = Utils.random(this.game.seeder[this.side]) * 4 - 10;
            on_side = 1 - on_side;
        }
        cnt++;
        if (this.game.bases[on_side] != null) {
            move(this.game.bases[on_side].base_move_x, this.game.bases[on_side].base_move_y);
        }
        ys = ys + 1;
        drop_y = y + ys;
        if (drop_y >= 566) {
            drop_y = 566;
            jump_flg = 2;
        } else {
            while (this.game.wall[on_side].hitTestPoint(x, drop_y + 15)) {
                jump_flg = 0;
                drop_y -= 1;
                ys = 0;
            }
        }
        y = drop_y;
        ySync();
        return true;
    }

    public boolean ground(){    //地面
        cnt++;
        if (this.game.wall[1 - side].hitTestPoint(x, y)) {
            this.game.dead_last[side] = type;
            kill();
            return false;
        }
        return true;
    }

    public boolean jump(){      //突击中
        ys += 0.32F;
        x = x + xs;
        y = y + ys;
        xySync();
        if (y >= 566) {
            y = 566;
            xs = 0;
            jump_flg = 2;
        }
        else if (this.game.wall[on_side].hitTestPoint(x, y + 15)) {
            jump_flg = 0;
        }
        else if (this.game.wall[on_side].hitTestPoint(x + (xs < 0 ? -16 : 16), y)) {
            xs = 0;
        }
        if (x < 0 || x > 1920) {
            kill();
            return false;
        }
        return true;
    }

    public void hurt(boolean is_crash){
    }   //受伤

    public void snipe(){
        int wrk_target = (int) Math.round(Math.toDegrees(Math.atan2(this.game.core_y[1 - side] - y, this.game.core_x[1 - side] - x)));
        if (rot - 180 > wrk_target) {
            wrk_target = wrk_target + 360;
        } else if (rot + 180 < wrk_target) {
            wrk_target = wrk_target - 360;
        }
        if (rot > wrk_target) {
            --rot;
            cos_rot = Utils.cos(rot);
            sin_rot = Utils.sin(rot);
        } else if (rot < wrk_target) {
            ++rot;
            cos_rot = Utils.cos(rot);
            sin_rot = Utils.sin(rot);
        }
        if (rot > 360) {
            rot = rot - 360;
        } else if (rot < 0) {
            rot = rot + 360;
        }
    }

    public void turn(int degree){
        rot = rot + (on_side == 0 ? 1 : -1) * degree;
        cos_rot = Utils.cos(rot);
        sin_rot = Utils.sin(rot);
        if (rot > 360) {
            rot = rot - 360;
        }
        else if (rot < 0) {
            rot = rot + 360;
        }
    }
}
