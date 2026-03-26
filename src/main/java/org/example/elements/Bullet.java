package org.example.elements;

import org.example.Game;
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
    protected final Game game;

    public Bullet(Game game, float X, float Y, int S) {
        super(X, Y, 15.5F);
        this.game = game;
        xySync();
        side = S;
        id = this.game.addElement(this);
        this.game.atk[side].addShape(this);
    }

    public void kill() {
        this.game.elements.remove(id);
        this.game.atk[side].removeShape(this);
    }

    @Override
    public void step(){
        if (y > 570) {
            if (!hit_ground()) return;
        }
        if (this.game.team[1-side].hitTestPoint(x, y) || gei_flg == 2) {
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
        new HitsDrop(this.game, x, y, this.game.atk[side]);
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
        xs = Utils.cos(from_rot) * 10F;
        ys = Utils.sin(from_rot) * 10F;
    }

    public void betray(){
        this.game.atk[side].removeShape(this);
        side = 1 - side;
        this.game.atk[side].addShape(this);
    }
}
