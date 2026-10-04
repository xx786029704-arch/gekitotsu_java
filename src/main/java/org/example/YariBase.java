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
}
