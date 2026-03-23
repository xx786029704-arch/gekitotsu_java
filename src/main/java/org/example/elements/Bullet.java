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

    public Bullet(float X, float Y, int S) {
        super(X, Y, 15.5F);
        xySync();
        side = S;
        id = Main.addElement(this);
        Main.atk[side].addShape(this);
    }

    public void kill() {
        Main.elements.remove(id);
        Main.atk[this.side].removeShape(this);
    }

    @Override
    public void step(){
        if (this.y > 570) {
            if (!hit_ground()) return;
        }
        if (Main.team[1-this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            if (!hit()) return;
        }
        move();
    }

    public boolean hit_ground(){    //撞击地面
        kill();
        return false;
    }

    public Bullet move(){      //移动
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        xySync();
        this.ys = this.ys + gravity;
        return this;
    }

    public boolean hit(){   //被摧毁
        new HitsDrop(this.x, this.y, Main.atk[side]);
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
        return this;
    }

    public Bullet setVecR(int r, float power){      //设置运动向量，极坐标版
        setVec(Utils.cos(r) * power, Utils.sin(r) * power);
        return this;
    }

    public Bullet setGravity(float g){    //设置重力
        gravity = g;
        return this;
    }
}
