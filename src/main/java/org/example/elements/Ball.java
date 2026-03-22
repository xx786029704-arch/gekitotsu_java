package org.example.elements;

import org.example.Main;
import org.example.Round;

public class Ball extends Round {       //兵玉基类
    public int side;
    public int type;
    public int cnt = 0;
    public int jump_flg = 0;
    public int speed = 0;
    public int on_side;
    public int hp = 10;
    public int max_hp = 10;
    public float rot;
    public float drop_y = 0;
    public float xs = 0;
    public float ys = 0;
    public float rot_radius;    //角度（弧度制）可以提前算好，遇到旋转壁再更新
    public float cos_rot;
    public float sin_rot;


    public Ball(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, 23.25F);
        xySync();
        this.side = S;
        this.on_side = S;
        this.rot = R;
        this.type = TYPE;
        this.rot_radius = R * 0.017453292519943295F;
        this.cos_rot = (float) Math.cos(rot_radius);
        this.sin_rot = (float) Math.sin(rot_radius);
        id = Main.addElement(this);
        Main.unit[side].addShape(this);
    }

    public void kill() {
        Main.elements.remove(id);
        Main.unit[this.side].removeShape(this);
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
        if (jump_flg == 0 && Main.jump_u[side].hitTestPoint(x, y)){
            jump_flg = 1;
            xs = 4 - 8 * on_side;
            ys = -8;
            on_side = 1 - on_side;
        }
        else if(jump_flg == 0 && Main.jump_f[side].hitTestPoint(x, y)){
            jump_flg = 1;
            xs = 15 - 30 * on_side;
            ys = -4;
            on_side = 1 - on_side;
        }
        else if(Main.snipe[side].hitTestPoint(x, y + 16)){
            snipe();
        }
        else if(Main.turn_ccw[side].hitTestPoint(x, y + 16)){
            turn(-2);
        }
        else if(Main.turn_cw[side].hitTestPoint(x, y + 16)){
            turn(2);
        }
        if (Main.atk[1 - side].hitTestPoint(x - 8, y - 8) || Main.atk[1 - side].hitTestPoint(x + 8, y - 8) || Main.atk[1 - side].hitTestPoint(x - 8, y + 8) || Main.atk[1 - side].hitTestPoint(x + 8, y + 8) || jump_flg == 0 && Main.dokkan_flg[on_side]) {
            hurt(Main.dokkan_flg[on_side]);     //从又臭又长的switch()改为调用自己的hurt方法
            hp--;
        }
        if (hp <= 0 || Main.hp0_flg[on_side] > 0 && jump_flg == 0) {
            kill();
            return;
        }

        if (hp < max_hp)
        {
            if (Main.heal[side].hitTestPoint(x, y))
            {
                hp++;
            }
        }
        stepEx();
    }

    public void stepEx(){   //如果基础行动判定通过则进行的额外行为

    }

    public boolean land(){      //正常
        if (jump_flg == 0 && !(side == on_side) && Main.hp0_flg[on_side] > 0) {
            jump_flg = 1;
            xs = (float) Math.random() * 7 - 3;
            ys = (float) Math.random() * 4 - 10;
            on_side = 1 - on_side;
        }
        cnt++;
        move(Main.bases[on_side].base_move_x, Main.bases[on_side].base_move_y);
        ys = ys + 1;
        drop_y = y + ys;
        if (drop_y >= 566) {
            drop_y = 566;
            jump_flg = 2;
        } else {
            while (Main.wall[on_side].hitTestPoint(x, drop_y + 15)) {
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
        if (Main.wall[1 - side].hitTestPoint(x, y))
        {
            kill();
            return false;
        }
        return true;
    }

    public boolean jump(){      //突击中
        ys += 0.32F;
        x = x + xs;
        y = y + ys;
        ySync();
        if (y >= 566) {
            y = 566;
            xs = 0;
            jump_flg = 2;
        }
        else if (Main.wall[on_side].hitTestPoint(x, y + 15)) {
            jump_flg = 0;
        }
        else if (Main.wall[on_side].hitTestPoint(x + (xs < 0 ? -16 : 16), y)) {
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

    public void updateRadiusCache(){
        this.rot_radius = rot * 0.017453292519943295F;
        this.cos_rot = (float) Math.cos(rot_radius);
        this.sin_rot = (float) Math.sin(rot_radius);
    }

    public void snipe(){
        int wrk_target = (int) Math.round(Math.toDegrees(Math.atan2(Main.core_y[1 - side] - y, Main.core_x[1 - side] - x)));
        if (rot - 180 > wrk_target) {
            wrk_target = wrk_target + 360;
        } else if (rot + 180 < wrk_target) {
            wrk_target = wrk_target - 360;
        }
        if (rot > wrk_target) {
            --rot;
            rot_radius = rot * 0.017453292519943295F;
            cos_rot = (float) Math.cos(rot_radius);
            sin_rot = (float) Math.sin(rot_radius);
        } else if (rot < wrk_target) {
            ++rot;
            rot_radius = rot * 0.017453292519943295F;
            cos_rot = (float) Math.cos(rot_radius);
            sin_rot = (float) Math.sin(rot_radius);
        }
        if (rot > 360) {
            rot = rot - 360;
        } else if (rot < 0) {
            rot = rot + 360;
        }
    }

    public void turn(int degree){
        rot = rot + (on_side == 0 ? 1 : -1) * degree;
        rot_radius = rot * 0.017453292519943295F;
        cos_rot = (float) Math.cos(rot_radius);
        sin_rot = (float) Math.sin(rot_radius);
        if (rot > 360) {
            rot = rot - 360;
        }
        else if (rot < 0) {
            rot = rot + 360;
        }
    }
}
