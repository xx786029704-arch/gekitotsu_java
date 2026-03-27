package org.example.elements.hit;

import org.example.GameTask;
import org.example.Main;
import org.example.Shape;
import org.example.Utils;
import org.example.elements.units.MagicBall;
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
    private final int side;
    private int steps;
    private int cnt;
    private static final boolean visible=true;//是否开启显示
    private final int user;
    private final GameTask game;

    public StarPrepareLaser(GameTask GAME, float X, float Y, int RDeg, int S, int U) {
        super(X, Y);
        game = GAME;
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
        id = game.addElement(this);
    }

    public void kill() {
        game.elements.remove(id);
    }

    protected float internalCos(int RDeg) {
        return Utils.cos(RDeg);
    }

    protected float internalSin(int RDeg) {
        return Utils.sin(RDeg);
    }

    @Override
    public void step() {
        cnt++;
        if (!visible && cnt < 30) return;
        Shape s = game.elements.get(user);
        if (s instanceof StarBall wrk) {
            rotDeg = wrk.rot;
            startX = wrk.x + 28.f * Utils.cos(rotDeg);
            startY = wrk.y + 28.f * Utils.sin(rotDeg);
            if (rotDeg != oldRotDeg) {
                oldRotDeg = rotDeg;
                xSpeed = Utils.cos((int) rotDeg) * 10.F;
                ySpeed = Utils.sin((int) rotDeg) * 10.F;
            }
            x = startX;
            y = startY;
            xySync();
            steps = 0;
            while (steps < 1000) {
                x = x + xSpeed;
                y = y + ySpeed;
                xySync();
                if (y < -600 || x > 1920 || x < 0) {
                    steps = -1;
                    break;
                } else if (y > 570 || game.wall[1 - side].hitTestPoint(x, y) || game.shield[1 - side].hitTestPoint(x, y))
                    break;
                steps++;
            }
            if (cnt == 30 && steps >= 0) {
                new StarLaser(game, x, side);
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
}