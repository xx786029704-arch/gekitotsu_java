package org.example.elements.hit;

import org.example.Main;
import org.example.Shape;
import org.example.Utils;
import org.example.elements.units.StarBall;

import java.awt.*;
import java.awt.geom.Path2D;

public class StarPrepareLaser extends Shape {     //星玉视觉激光类

    public int rotDeg;
    private float xSpeed;
    private float ySpeed;
    protected float startX;
    protected float startY;
    private float oldRotDeg;
    private int side;
    private int steps;
    private int cnt;
    private static boolean visible=true;//是否开启显示
    private int user;

    public StarPrepareLaser(float X, float Y, int RDeg, int S,int U) {
        super(X, Y);
        rotDeg = RDeg;
        xSpeed = 0.F;
        ySpeed = 10.F;
        startX = X;
        startY = Y;
        oldRotDeg = 90.F;
        side = S;
        steps = -1;
        user = U;
        cnt = 0;
        id = Main.addElement(this);
    }

    public void kill() {
        Main.elements.remove(id);
    }

    protected float internalCos(int RDeg) {
        return Utils.cos(RDeg);
    }

    protected float internalSin(int RDeg) {
        return Utils.sin(RDeg);
    }

    @Override
    public void step() {
        this.cnt++;
        if (!visible && this.cnt < 30) return;
        if (Main.elements.containsKey(user)) {
            //此处代码缺乏安全性，但一般应该能保证user一定是StarBall
            StarBall wrk = (StarBall) (Main.elements.get(user));
            this.rotDeg = wrk.rot;
            this.startX = wrk.x + 28.f * Utils.cos(this.rotDeg);
            this.startY = wrk.y + 28.f * Utils.sin(this.rotDeg);
            if (this.rotDeg != this.oldRotDeg) {
                this.oldRotDeg = this.rotDeg;
                this.xSpeed = Utils.cos((int) this.rotDeg) * 10.F;
                this.ySpeed = Utils.sin((int) this.rotDeg) * 10.F;
            }
            this.x = this.startX;
            this.y = this.startY;
            xySync();
            this.steps = 0;
            while (this.steps < 1000) {
                this.x = this.x + this.xSpeed;
                this.y = this.y + this.ySpeed;
                xySync();
                if (this.y < -600 || this.x > 1920 || this.x < 0) {
                    this.steps = -1;
                    break;
                } else if (this.y > 570 || Main.wall[1 - this.side].hitTestPoint(this.x, this.y) || Main.shield[1 - this.side].hitTestPoint(this.x, this.y))
                    break;
                this.steps++;
            }
            if (this.cnt == 30 && this.steps >= 0) {
                new StarLaser(this.x, this.side);
            }
            if (!wrk.shooting) {
                kill();
            }
        } else {
            kill();
        }
    }

    @Override
    public boolean hitTestPoint(float X, float Y) {
        return false;
    }


    @Override
    public void draw(Graphics2D g2d) {
        if (this.visible && this.steps >= 0) {
            g2d.setColor(Color.MAGENTA);
            g2d.drawLine((int) startX, (int) startY, (int) x, (int) y);
            g2d.setColor(Color.WHITE);
        }
    }
}