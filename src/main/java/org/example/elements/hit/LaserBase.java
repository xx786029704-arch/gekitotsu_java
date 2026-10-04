package org.example.elements.hit;

import org.example.GameTask;
import org.example.Shape;

import java.awt.*;
import java.awt.geom.Path2D;

public class LaserBase extends Shape {     //激光类

    public float rotDeg;
    private final float thickness;    //激光的宽度
    protected float circleRadius; //激光末端圆圈半径
    protected float xSpeed;
    protected float ySpeed;
    protected float startX;
    protected float startY;
    private float oldRotDeg;
    protected int side;
    protected int steps;
    private int id;
    protected final GameTask game;

    public LaserBase(GameTask GAME, float X, float Y, int RDeg, int S,float thi, float radi) {
        super(X, Y);
        game = GAME;
        rotDeg = RDeg;
        thickness = thi;
        circleRadius = radi;
        xSpeed = 0.F;
        ySpeed = 10.F;
        startX = X;
        startY = Y;
        oldRotDeg = 90.F;
        side = S;
        steps = 0;
        id = game.addElement(this);
        game.atk[side].addShape(this);
    }

    public void kill() {
        game.elements.remove(id);
        game.atk[side].removeShape(this);
    }

    protected float internalCos(float RDeg){
        return 0.F;
    }

    protected float internalSin(float RDeg){
        return 1.F;
    }

    protected void follow() { //请重写该函数，包含跟随主体移动和旋转的逻辑，以及何时消失的逻辑
        return;
    }

    @Override
    public void step() {
        follow();
        if (this.rotDeg != this.oldRotDeg) {
            this.oldRotDeg = this.rotDeg;
            this.xSpeed = this.internalCos(this.rotDeg) * 10.F;
            this.ySpeed = this.internalSin(this.rotDeg) * 10.F;
        }
        this.x = this.startX;
        this.y = this.startY;
        xySync();
        this.steps = 0;
        while (this.steps < 1000) {
            this.x = this.x + this.xSpeed;
            this.y = this.y + this.ySpeed;
            xySync();
            if (this.y > 570 || this.y < -600 || this.x > 1920 || this.x < 0) break;
            else if (game.shield[1 - this.side].hitTestPoint(this.x, this.y) || game.fort[1 - this.side].hitTestPoint(this.x, this.y))
                break;
            this.steps++;
        }
    }

    @Override
    public boolean hitTestPoint(float X, float Y) {
        float dx = X - this.startX, dy = Y - this.startY;
        float wrk = dx * this.xSpeed + dy * this.ySpeed;
        if (wrk >= 0.f && wrk <= steps * 100.f) {
            if (Math.abs(dy * this.xSpeed - dx * this.ySpeed) <= this.thickness * 5.f) return true;
        }
        dx = X - this.x;
        dy = Y - this.y;
        return dx * dx + dy * dy <= this.circleRadius * this.circleRadius;
    }
}