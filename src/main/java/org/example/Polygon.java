package org.example;

import java.awt.*;
import java.awt.geom.Path2D;

public class Polygon extends Shape{     //多边形，存储各个顶点坐标数据
    public final float[][] localVertices;
    protected float minX;
    protected float maxX;
    protected float minY;
    protected float maxY;

    /*
     可以再做一个特化的矩形类，只存储坐标和长宽，
     在点碰撞检测中比多边形更快
     对要塞壁应该有明显优化    *后续：蛤蛤，我要塞壁神之一手天才优化，完全之不需要了
     后续有需求可以做
     */

    public Polygon(float X, float Y, float[][] vertices) {
        super(X, Y);
        this.localVertices = vertices;
        for (int i = 1; i < vertices.length; i++) {
            if (vertices[i][0] < minX) minX = vertices[i][0];
            if (vertices[i][0] > maxX) maxX = vertices[i][0];
            if (vertices[i][1] < minY) minY = vertices[i][1];
            if (vertices[i][1] > maxY) maxY = vertices[i][1];
        }
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
    public boolean hitTestPoint(float X, float Y) {
        X -= x;
        Y -= y;
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
    public void draw(Graphics2D g2d) {
        g2d.translate(x, y);
        if (localVertices.length == 0) return;
        Path2D.Float path = new Path2D.Float();
        path.moveTo(localVertices[0][0], localVertices[0][1]);
        for (int i = 1; i < localVertices.length; i++) {
            path.lineTo(localVertices[i][0], localVertices[i][1]);
        }
        path.closePath();
        g2d.draw(path);
        g2d.translate(-x, -y);
    }
}
