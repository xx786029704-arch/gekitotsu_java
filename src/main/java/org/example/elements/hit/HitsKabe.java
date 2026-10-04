package org.example.elements.hit;

import org.example.GameTask;
import org.example.Polygon;
import org.example.Main;
import org.example.Shape;
import org.example.elements.units.KabeBall;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;

public class HitsKabe extends Polygon {
    private final int side;
    private int frame;
    private final int user;
    private float cos_rot;
    private float sin_rot;
    private GameTask game;
    private static final float[][] baseVerts={
            {62.8f,-.05f},
            {125.6f,72.5f},
            {31.35f,54.35f},
            {0.f,145.f},
            {-31.45f,54.35f},
            {-125.6f,72.5f},
            {-62.85f,0.f},
            {-125.6f,-72.55f},
            {-31.45f,-54.4f},
            {-.05f,-145.f},
            {31.35f,-54.4f},
            {125.6f,-72.55f}
    };
    private static final float[] baseScaleInverse={
            4.027777777777778F,
            2.013888888888889F,
            1.342592592592593F,
            1.006944444444444F,
            0.805555555555556F,
            0.833333333333333F,
            0.863095238095238F,
            0.833333333333333F
    };
    public HitsKabe(GameTask GAME,  float X, float Y, float R, int S, int USER) {
        super(X, Y,baseVerts);
        xySync();
        game = GAME;
        frame = 0;
        user = USER;
        KabeBall wrk = (KabeBall) (game.elements.get(user));
        cos_rot = wrk.cos_rot;
        sin_rot = wrk.sin_rot;
        side = S;
        id = game.addElement(this);
        game.shield[side].addShape(this);
    }

    public void kill() {
        game.elements.remove(id);
        game.shield[side].removeShape(this);
    }

    @Override
    public void step(){
        Shape s = game.elements.get(user);
        if (s instanceof KabeBall wrk) {
            x = wrk.x + 20 * wrk.cos_rot;
            y = wrk.y + 20 * wrk.sin_rot;
            cos_rot = wrk.cos_rot;
            sin_rot = wrk.sin_rot;
            xySync();
            if (!wrk.shooting) {
                kill();
                return;
            }
        }
        else{
            kill();
            return;
        }
        frame++;
        if (frame>=8) frame=4;
    }

    @Override
    public boolean hitTestPoint(float X, float Y) {
        X -= x;
        Y -= y;
        float old_X = X;
        X = (X * cos_rot + Y * sin_rot);
        Y = (Y * cos_rot - old_X * sin_rot);
        X *= baseScaleInverse[this.frame];
        Y *= baseScaleInverse[this.frame];
        if (maxX < X || minX > X || maxY < Y || minY > Y){
            return false;
        }
        boolean hit = false;
        int j = localVertices.length - 1;
        for (int i = 0; i < localVertices.length; i++) {
            float xi = localVertices[i][0], yi = localVertices[i][1];
            float xj = localVertices[j][0], yj = localVertices[j][1];
            if (((yi > Y) != (yj > Y)) &&
                    (X < (xj - xi) * (Y - yi) / (yj - yi) + xi)) {
                hit = !hit;
            }
            j = i;
        }
        return hit;
    }
}