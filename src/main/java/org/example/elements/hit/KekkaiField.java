package org.example.elements.hit;

import org.example.Main;
import org.example.Shape;
import org.example.elements.units.KekkaiBall;

import java.awt.*;
import java.util.List;

public class KekkaiField extends Shape {        //界玉结界（gemini优化版)
    private static final float LINE_WIDTH = 30F;
    private static final float HALF_WIDTH = 15F;
    private static final float LIMIT_SQ = HALF_WIDTH * HALF_WIDTH;

    private final int side;

    // 1. 数组扁平化：彻底消灭 ArrayList 和 float[] 对象的每帧分配 (Zero GC)
    private float[] pointsX = new float[4]; // 初始容量，会自动扩容
    private float[] pointsY = new float[4];
    private int activePoints = 0;

    // 2. AABB 快速剔除边界
    private float minX, maxX, minY, maxY;

    // 3. 渲染对象缓存
    private final Stroke cachedStroke = new BasicStroke(LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private final Color cachedColor;

    public KekkaiField(int side) {
        super(0, 0);
        this.side = side;
        this.id = Main.addElement(this);
        this.cachedColor = (side == 0) ? new Color(0, 255, 255, 120) : new Color(255, 128, 0, 120);
        Main.shield[side].addShape(this);
    }

    @Override
    public void step() {
        rebuildSegments();
    }

    private void rebuildSegments() {
        List<Integer> ids = Main.kekkaiIds[side];
        int size = ids.size();

        // 确保数组容量充足，避免越界
        if (pointsX.length < size) {
            pointsX = new float[size * 2];
            pointsY = new float[size * 2];
        }

        activePoints = 0;
        float tMinX = Float.MAX_VALUE, tMaxX = -Float.MAX_VALUE;
        float tMinY = Float.MAX_VALUE, tMaxY = -Float.MAX_VALUE;

        for (int i = 0; i < size; i++) {
            // 4. 优化 Map 查找：一次 get 搞定，干掉 containsKey
            Shape s = Main.elements.get(ids.get(i));
            if (s instanceof KekkaiBall ball) {
                if (ball.jump_flg == 0 && ball.side == ball.on_side && ball.hurt_time <= 0) {
                    float px = ball.x + ball.cos_rot * 33F;
                    float py = ball.y + ball.sin_rot * 33F;

                    pointsX[activePoints] = px;
                    pointsY[activePoints] = py;
                    activePoints++;

                    // 维护 AABB 包围盒
                    if (px < tMinX) tMinX = px;
                    if (px > tMaxX) tMaxX = px;
                    if (py < tMinY) tMinY = py;
                    if (py > tMaxY) tMaxY = py;
                }
            }
        }

        // 加上线宽半径作为最终 AABB 边界
        this.minX = tMinX - HALF_WIDTH;
        this.maxX = tMaxX + HALF_WIDTH;
        this.minY = tMinY - HALF_WIDTH;
        this.maxY = tMaxY + HALF_WIDTH;
    }

    @Override
    public boolean hitTestPoint(float X, float Y) {
        if (activePoints < 2) return false;

        // 5. Broad-Phase AABB 快速剔除：如果点在结界包围盒外，直接 O(1) 结束！
        if (X < minX || X > maxX || Y < minY || Y > maxY) {
            return false;
        }

        // 6. 干掉 Segment 数组，直接使用点数组进行循环，并手动内联数学运算以榨干方法调用开销
        for (int i = 0; i < activePoints; i++) {
            float x1 = pointsX[i];
            float y1 = pointsY[i];
            // 取消耗时的取模运算 (%)
            int nextIdx = (i + 1 == activePoints) ? 0 : i + 1;
            float x2 = pointsX[nextIdx];
            float y2 = pointsY[nextIdx];

            // 以下为内联的 distanceToSegmentSquared
            float vx = x2 - x1;
            float vy = y2 - y1;
            float wx = X - x1;
            float wy = Y - y1;
            float c1 = vx * wx + vy * wy;

            float distSq;
            if (c1 <= 0) {
                float dx = X - x1;
                float dy = Y - y1;
                distSq = dx * dx + dy * dy;
            } else {
                float c2 = vx * vx + vy * vy;
                if (c2 <= c1) {
                    float dx = X - x2;
                    float dy = Y - y2;
                    distSq = dx * dx + dy * dy;
                } else {
                    float b = c1 / c2;
                    float bx = x1 + b * vx;
                    float by = y1 + b * vy;
                    float dx = X - bx;
                    float dy = Y - by;
                    distSq = dx * dx + dy * dy;
                }
            }

            if (distSq <= LIMIT_SQ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void draw(Graphics2D g2d) {
        if (activePoints < 2) return;
        Stroke prev = g2d.getStroke();
        Color prevColor = g2d.getColor();
        // 使用缓存的对象
        g2d.setStroke(cachedStroke);
        g2d.setColor(cachedColor);
        for (int i = 0; i < activePoints; i++) {
            int nextIdx = (i + 1 == activePoints) ? 0 : i + 1;
            g2d.drawLine((int) pointsX[i], (int) pointsY[i], (int) pointsX[nextIdx], (int) pointsY[nextIdx]);
        }
        g2d.setStroke(prev);
        g2d.setColor(prevColor);
    }
}
