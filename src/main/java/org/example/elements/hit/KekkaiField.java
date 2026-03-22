package org.example.elements.hit;

import org.example.Main;
import org.example.Shape;
import org.example.elements.units.KekkaiBall;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class KekkaiField extends Shape {   //界玉的结界
    private static final float LINE_WIDTH = 30F;
    private static final float HALF_WIDTH = LINE_WIDTH * 0.5F;
    private final int side;
    private final List<float[]> points = new ArrayList<>();
    private final List<float[]> segments = new ArrayList<>();

    public KekkaiField(int side) {   //初始化
        super(0, 0);
        this.side = side;
        this.id = Main.addElement(this);
        Main.shield[side].addShape(this);
    }

    @Override
    public void step() {   //每帧更新结界线段
        rebuildSegments();
    }

    private void rebuildSegments() {
        points.clear();
        segments.clear();
        List<Integer> ids = Main.kekkaiIds[side];
        for (int i = 0; i < ids.size(); i++) {
            Integer id = ids.get(i);
            if (!Main.elements.containsKey(id)) {
                continue;
            }
            if (!(Main.elements.get(id) instanceof KekkaiBall)) {
                continue;
            }
            KekkaiBall ball = (KekkaiBall) Main.elements.get(id);
            if (ball.jump_flg != 0 || ball.side != ball.on_side || ball.hurt_time > 0) {
                continue;
            }
            float px = ball.x + ball.cos_rot * 33F;
            float py = ball.y + ball.sin_rot * 33F;
            points.add(new float[]{px, py});
        }
        if (points.size() < 2) {
            return;
        }
        for (int i = 0; i < points.size(); i++) {
            float[] p1 = points.get(i);
            float[] p2 = points.get((i + 1) % points.size());
            segments.add(new float[]{p1[0], p1[1], p2[0], p2[1]});
        }
    }

    @Override
    public Boolean hitTestPoint(float X, float Y) {   //点碰撞
        if (segments.isEmpty()) {
            return false;
        }
        float limit2 = HALF_WIDTH * HALF_WIDTH;
        for (int i = 0; i < segments.size(); i++) {
            float[] s = segments.get(i);
            if (distanceToSegmentSquared(X, Y, s[0], s[1], s[2], s[3]) <= limit2) {
                return true;
            }
        }
        return false;
    }

    private float distanceToSegmentSquared(float px, float py, float x1, float y1, float x2, float y2) {
        float vx = x2 - x1;
        float vy = y2 - y1;
        float wx = px - x1;
        float wy = py - y1;
        float c1 = vx * wx + vy * wy;
        if (c1 <= 0) {
            float dx = px - x1;
            float dy = py - y1;
            return dx * dx + dy * dy;
        }
        float c2 = vx * vx + vy * vy;
        if (c2 <= c1) {
            float dx = px - x2;
            float dy = py - y2;
            return dx * dx + dy * dy;
        }
        float b = c1 / c2;
        float bx = x1 + b * vx;
        float by = y1 + b * vy;
        float dx = px - bx;
        float dy = py - by;
        return dx * dx + dy * dy;
    }

    @Override
    public void draw(Graphics2D g2d) {
        if (segments.isEmpty()) {
            return;
        }
        Stroke prev = g2d.getStroke();
        Color prevColor = g2d.getColor();
        g2d.setStroke(new BasicStroke(LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(side == 0 ? new Color(0, 255, 255, 120) : new Color(255, 128, 0, 120));
        for (int i = 0; i < segments.size(); i++) {
            float[] s = segments.get(i);
            g2d.drawLine((int) s[0], (int) s[1], (int) s[2], (int) s[3]);
        }
        g2d.setStroke(prev);
        g2d.setColor(prevColor);
    }
}
