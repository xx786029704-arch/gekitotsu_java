package org.example.elements;

import org.example.Main;
import org.example.Round;
import org.example.elements.hit.HitsDrop;

public class Bullet extends Round {     //子弹基类
    public int id;
    public int side;
    public int gei_flg = 0;
    public float xs = 0;
    public float ys = 0;
    public float rot = 0;
    public float gravity = 0.32F;

    public Bullet(float X, float Y, int S) {
        super(X, Y, 15.5F);
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
        if (!move()) return;
    }

    public boolean hit_ground(){    //撞击地面
        kill();
        return false;
    }

    public boolean move(){      //移动
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        this.ys = this.ys + gravity;
        return true;
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

    public Bullet setVecR(float r, float power){      //设置运动向量，极坐标版
        setVec((float) Math.cos(r) * power, (float) Math.sin(r) * power);
        return this;
    }

    public Bullet setGravity(float g){    //设置重力
        gravity = g;
        return this;
    }
}
