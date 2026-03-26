package org.example.elements.hit;

import org.example.Polygon;
import org.example.Game;
import org.example.Utils;
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
    @Deprecated
    private float rot_radius;
    protected final Game game;
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
    public HitsKabe(Game game, float X, float Y, float R, int S, int USER) {
        super(X, Y,baseVerts);
        this.game = game;
        xySync();
        frame = 0;
        user = USER;
        KabeBall wrk = (KabeBall) (this.game.elements.get(user));
        cos_rot = wrk.cos_rot;
        sin_rot = wrk.sin_rot;
        rot_radius = R * 0.017453292519943295F;
        side = S;
        id = this.game.addElement(this);
        this.game.shield[side].addShape(this);
    }

    public void kill() {
        this.game.elements.remove(id);
        this.game.shield[side].removeShape(this);
    }

    @Override
    public void step(){
        if (this.game.elements.containsKey(user)) {
            //此处代码缺乏安全性，但一般应该能保证user一定是KabeBall
            KabeBall wrk = (KabeBall) (this.game.elements.get(user));
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

    @Override
    public void draw(Graphics2D g2d) {//调试用 用完删掉
        AffineTransform oldAT = g2d.getTransform();
        g2d.translate(x, y);
        g2d.rotate(rot_radius, 0, 0);
        g2d.scale(1/baseScaleInverse[this.frame], 1/baseScaleInverse[this.frame]);
        if (localVertices.length == 0) return;
        Path2D.Float path = new Path2D.Float();
        path.moveTo(localVertices[0][0], localVertices[0][1]);
        for (int i = 1; i < localVertices.length; i++) {
            path.lineTo(localVertices[i][0], localVertices[i][1]);
        }
        path.closePath();
        g2d.draw(path);
        g2d.setTransform(oldAT);
    }
}