package org.example;

import java.awt.*;
import java.awt.geom.Path2D;

public class YariBase extends Shape {     //枪玉判定图形单元类

    //基本图形先缩放，再平移，
    public int flipRad;
    private final float internalScaleX;    //单元本身的缩放尺寸
    private final float internalX;
    private final float internalY;
    private final boolean flipped;
    private final float cosRot;
    private final float sinRot;

    public YariBase(float X, float Y, int RDeg, float iSX, float iX,float iY) {
        super(X, Y);
        flipped = !(RDeg < 90 || RDeg > 270);
        flipRad = flipped ? (RDeg + 180) % 360 : RDeg;
        internalX = iX;
        internalScaleX = iSX;
        internalY = iY;
        cosRot = Utils.cos(flipRad);
        sinRot = Utils.sin(flipRad);
    }

    @Override
    public boolean hitTestPoint(float X, float Y) {
        //先把测试点反向旋转本对象的角度，然后如果有翻转再翻转，这样就得到了相当于该判定是无翻转旋转的时候的测试点位置
        float rawDx = X - this.x;
        float rawDy = Y - this.y;
        //反向旋转
        float dx = rawDx * cosRot + rawDy * sinRot;
        float dy = rawDy * cosRot - rawDx * sinRot;
        //翻转
        if (flipped) dx = -dx;
        /////////////////////以下为无翻转旋转的判定逻辑
        dx = (dx - internalX) / internalScaleX;
        dy = dy - internalY;
        if (dx < 76.f || dx > 156.f || dy < 0.2f * (dx - 156.f) || dy > -.2f * (dx - 156.f)) return false;
        return dx * dx / 7744.f + dy * dy / 1004.89f > 1;
    }


    @Override
    public void draw(Graphics2D g2d) {
        float[][] verts = {
                {76.f, -16.f},
                {156.f, 0.f},
                {76.f, 16.f},
                {85.f, 8.f},
                {88.f, 0.f},
                {85.f, -8.f}
        };
        for (float[] point : verts) {
            float tempX = point[0] * internalScaleX + internalX;
            if (flipped) tempX = -tempX;
            float tempY = point[1] + internalY;
            point[0] = tempX * cosRot - tempY * sinRot + this.x;
            point[1] = tempY * cosRot + tempX * sinRot + this.y;
        }
        //!!!前述部分结束
        Path2D.Float path = new Path2D.Float();
        path.moveTo(verts[0][0], verts[0][1]);
        for (int i = 1; i < 6; i++) {
            path.lineTo(verts[i][0], verts[i][1]);
        }
        path.closePath();
        g2d.draw(path);
        //!!!此处代码用于验证实际判定区域是不是和显示的一样，调试后请删掉
        /* g2d.setColor(Color.CYAN);
        for (int i = -200; i < 200; i++) {
            for (int j = -200; j < 200; j++) {
                int testX = (int) this.x + i;
                int testY = (int) this.y + j;
                if (hitTestPoint(testX, testY)) {
                    g2d.drawRect(testX, testY, 1, 1);
                }
            }
        }
        g2d.setColor(Color.WHITE); */
        //!!!前述部分结束
    }
}
