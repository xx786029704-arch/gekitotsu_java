package org.example.elements;

import org.example.Main;
import org.example.Round;
import org.example.Utils;
import org.example.elements.hit.HitsDrop;

public class Bullet extends Round {     //子弹基类
    public int id;
    public int side;
    public int gei_flg = 1;
    public float xs = 0;
    public float ys = 0;
    public float gravity = 0;
    public float power = 0;

    public Bullet(float X, float Y, int S) {
        super(X, Y, 15.5F);
        xySync();
        side = S;
        id = Main.addElement(this);
        Main.atk[side].addShape(this);
    }

    public void kill() {
        Main.elements.remove(id);
        Main.atk[side].removeShape(this);
    }

    @Override
    public void step(){
        if (y > 570) {
            if (!hit_ground()) return;
        }
        if (Main.team[1-side].hitTestPoint(x, y) || gei_flg == 2) {
            if (!hit()) return;
        }
        move();
    }

    public boolean hit_ground(){    //撞击地面
        kill();
        return false;
    }

    public Bullet move(){      //移动
        x = x + xs;
        y = y + ys;
        xySync();
        ys = ys + gravity;
        return this;
    }

    public boolean hit(){   //被摧毁
        new HitsDrop(x, y, Main.atk[side]);
        kill();
        return false;
    }

    public Bullet setVec(float vx, float vy){     //设置运动向量
        xs = vx;
        ys = vy;
        return this;
    }

    public Bullet setVecMult(float vx, float vy, float mult){     //设置运动向量
        xs = vx * mult;
        ys = vy * mult;
        power = mult;
        return this;
    }

    public Bullet setVecR(int r, float speed){      //设置运动向量，极坐标版
        setVec(Utils.cos(r) * speed, Utils.sin(r) * speed);
        power = speed;
        return this;
    }

    public Bullet setGravity(float g){    //设置重力
        gravity = g;
        return this;
    }

    public Bullet setPower(float P){      //设置运动向量，极坐标版
        power = P;
        return this;
    }

    public void reflect(int from_rot){
        betray();
        xs = Utils.cos(from_rot) * power;
        ys = Utils.sin(from_rot) * power;
    }

    public void betray(){
        Main.atk[side].removeShape(this);
        side = 1 - side;
        Main.atk[side].addShape(this);
    }
}
