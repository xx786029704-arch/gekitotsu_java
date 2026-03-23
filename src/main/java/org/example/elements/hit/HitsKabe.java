package org.example.elements.hit;

import org.example.Polygon;
import org.example.Main;
import org.example.Utils;
import org.example.elements.units.KabeBall;
import java.awt.*;

public class HitsKabe extends Polygon {
    private int side;
    private int frame;
    private int user;
    private float rot;
    private static float[][] baseVerts={
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
    private static float[] baseScale={
            0.20689655172413793F,
            0.41379310344827586F,
            0.6206896551724139F,
            0.8275862068965517F,
            1.0344827586206897F,
            1.F,
            0.9655172413793104F,
            1.F
    };
    public HitsKabe(float X, float Y, float R, int S, int USER) {
        super(X, Y,new float[][]{});
        xySync();
        frame = 0;
        rot = (R % 360 + 360) % 360;
        user = USER;
        side = S;
        id = Main.addElement(this);
        Main.shield[side].addShape(this);
    }

    public void kill() {
        Main.elements.remove(id);
        Main.shield[side].removeShape(this);
    }

    @Override
    protected float[][] getWorldVertices() {      //获取顶点的世界绝对坐标
        float[][] worldVerts = new float[12][2];
        //需确保角度一定是整数
        for (int i = 0; i < 12; i++) {
            worldVerts[i][0] = (baseVerts[i][0] * Utils.cos((int) this.rot) - baseVerts[i][1] * Utils.sin((int) this.rot)) * baseScale[this.frame] + this.x;
            worldVerts[i][1] = (baseVerts[i][1] * Utils.cos((int) this.rot) + baseVerts[i][0] * Utils.sin((int) this.rot)) * baseScale[this.frame] + this.y;
        }
        return worldVerts;
    }

    @Override
    public void step(){
        if (Main.elements.containsKey(user)) {
            //此处代码缺乏安全性，但一般应该能保证user一定是KabeBall
            KabeBall wrk = (KabeBall) (Main.elements.get(user));
            this.x = wrk.x + 20 * Utils.cos((int) wrk.rot);
            this.y = wrk.y + 20 * Utils.sin((int) wrk.rot);
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

    /*
    @Override
    public void draw(Graphics2D g2d) {//调试用 用完删掉
        super.draw(g2d);
        g2d.setColor(Color.CYAN);
        for (int i = -200; i < 200; i++) {
            for (int j = -200; j < 200; j++) {
                int testX = (int) this.x + i;
                int testY = (int) this.y + j;
                if (hitTestPoint(testX, testY)) {
                    g2d.drawRect(testX, testY, 1, 1);
                }
            }
        }
        g2d.setColor(Color.WHITE);
    }

     */
}
