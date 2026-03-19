package org.example;

import java.awt.*;
import java.awt.geom.Path2D;

public class Polygon extends Shape{     //多边形，存储各个顶点坐标数据
    public float[][] localVertices;

    /*
     可以再做一个特化的矩形类，只存储坐标和长宽，
     在点碰撞检测中比多边形更快
     对要塞壁应该有明显优化
     后续有需求可以做
     */

    public Polygon(float X, float Y, float[][] vertices) {
        super(X, Y);
        this.localVertices = vertices;
    }

    private float[][] getWorldVertices() {      //获取顶点的世界绝对坐标
        float[][] worldVerts = new float[localVertices.length][2];
        for (int i = 0; i < localVertices.length; i++) {
            worldVerts[i][0] = localVertices[i][0] + this.x;
            worldVerts[i][1] = localVertices[i][1] + this.y;
        }
        return worldVerts;
    }

    @Override
    public Boolean hitTestPoint(float X, float Y) {
        float[][] verts = getWorldVertices();
        boolean hit = false;
        float minX = verts[0][0];
        float maxX = verts[0][0];
        float minY = verts[0][1];
        float maxY = verts[0][1];
        for (int i = 1; i < verts.length; i++) {
            if (verts[i][0] < minX) minX = verts[i][0];
            if (verts[i][0] > maxX) maxX = verts[i][0];
            if (verts[i][1] < minY) minY = verts[i][1];
            if (verts[i][1] > maxY) maxY = verts[i][1];
        }
        if (maxX < X || minX > X || maxY < Y || minY > Y){
            return false;
        }

        int j = verts.length - 1;
        for (int i = 0; i < verts.length; i++) {
            float xi = verts[i][0], yi = verts[i][1];
            float xj = verts[j][0], yj = verts[j][1];
            if (((yi > Y) != (yj > Y)) &&
                    (X < (xj - xi) * (Y - yi) / (yj - yi) + xi)) {
                hit = !hit;
            }
            j = i;
        }
        return hit;
    }

    @Override
    public void draw(Graphics2D g2d) {
        float[][] verts = getWorldVertices();
        if (verts.length == 0) return;
        Path2D.Float path = new Path2D.Float();
        path.moveTo(verts[0][0], verts[0][1]);
        for (int i = 1; i < verts.length; i++) {
            path.lineTo(verts[i][0], verts[i][1]);
        }
        path.closePath();
        g2d.draw(path);
    }
}
