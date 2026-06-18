package org.example.GUI;

import org.example.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 轨迹画布：缩放/平移/网格/要塞车/轨迹渲染。 */
public class TraceCanvas extends JPanel {
    private static final int BIAS_X = 400;
    private static final int BIAS_Y = 420;

    private final List<Trace> traces;
    private final List<TraceWall> walls;
    private final List<Variable> variables;
    private double scale = 1.0;
    private double offsetX = 0;
    private double offsetY = 0;
    private Point dragStart;

    boolean showCenter = true;
    boolean showLandingDetection = false;
    boolean showWallDetection = false;
    boolean showUnitOutline = false;
    boolean showFortOutline = false;

    public TraceCanvas(List<Trace> traces, List<TraceWall> walls, List<Variable> variables) {
        super(null);
        this.traces = traces;
        this.walls = walls;
        this.variables = variables;

        JButton resetBtn = new JButton("复原");
        resetBtn.setFont(new Font("黑体", Font.PLAIN, 13));
        resetBtn.setMargin(new java.awt.Insets(0, 6, 0, 6));
        resetBtn.setFocusable(false);
        resetBtn.addActionListener(e -> {
            scale = 1;
            offsetX = 0;
            offsetY = 0;
            repaint();
        });
        add(resetBtn);
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent e) {
                resetBtn.setBounds(getWidth() - 62, 6, 54, 22);
            }
        });

        // 滚轮缩放（以鼠标位置为锚点）
        addMouseWheelListener(e -> {
            double oldScale = scale;
            double factor = e.getPreciseWheelRotation() > 0 ? 0.9 : 1.1;
            scale = Math.clamp(scale * factor, 0.1, 5.0);
            double mx = e.getX();
            double my = e.getY();
            offsetX = mx - (mx - offsetX) * scale / oldScale;
            offsetY = my - (my - offsetY) * scale / oldScale;
            repaint();
        });

        // 拖拽平移
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                dragStart = e.getPoint();
            }
        });
        addMouseMotionListener(new java.awt.event.MouseAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                offsetX += e.getX() - dragStart.x;
                offsetY += e.getY() - dragStart.y;
                dragStart = e.getPoint();
                repaint();
            }
        });
    }

    void updateDarkMode() {
        setBackground(Main.DARK_MODE ? new Color(0x2D, 0x2D, 0x2D) : Color.WHITE);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();

        Map<String, Integer> varMap = Trace.varMap(variables);

        // 背景网格
        g2.setColor(Main.DARK_MODE ? new Color(0x3A, 0x3A, 0x3A) : new Color(0xEE, 0xEE, 0xEE));
        int gridSize = (int) (80 * scale);
        double ox = offsetX % gridSize;
        double oy = offsetY % gridSize;
        for (double gx = ox; gx < w; gx += gridSize)
            g2.drawLine((int) gx, 0, (int) gx, h);
        for (double gy = oy; gy < h; gy += gridSize)
            g2.drawLine(0, (int) gy, w, (int) gy);

        // 地面参考线
        double groundCanvasY = (50 + BIAS_Y) * scale + offsetY;
        g2.setColor(Main.DARK_MODE ? new Color(0x60, 0x60, 0x60) : new Color(0xCC, 0xCC, 0xCC));
        g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{4, 4}, 0));
        g2.drawLine(0, (int) groundCanvasY, w, (int) groundCanvasY);

        // 中轴参考线 (暂时弃用)
        int midLine = (int) (BIAS_X * scale + offsetX);
        //g2.drawLine(midLine, 0, midLine, h);

        // 要塞车
        g2.setColor(Main.DARK_MODE ? new Color(0xDD, 0xDD, 0xDD) : new Color(0x30, 0x30, 0x30));
        g2.setStroke(new BasicStroke(1));
        int base_y0 = (int) ((BIAS_Y - 15.5) * scale + offsetY);
        int base_x0 = (int) ((BIAS_X - 191.5) * scale + offsetX);
        int wheel_x0 = (int) ((BIAS_X - 140.2) * scale + offsetX);
        int wheel_x1 = (int) ((BIAS_X + 77.2) * scale + offsetX);
        int wheel_y = (int) ((BIAS_Y - 11.5) * scale + offsetY);
        g2.drawRoundRect(base_x0, base_y0, (int) (383 * scale), (int) (43 * scale), (int) (23 * scale), (int) (23 * scale));
        g2.drawOval(wheel_x0, wheel_y, (int) (63 * scale), (int) (63 * scale));
        g2.drawOval(wheel_x1, wheel_y, (int) (63 * scale), (int) (63 * scale));

        //阵型边界
        if (showFortOutline){
            g2.setColor(Main.DARK_MODE ? new Color(0x60, 0x60, 0x60) : new Color(0xCC, 0xCC, 0xCC));
            g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{4, 4}, 0));
            g2.drawRect((int) ((BIAS_X - 190) * scale + offsetX), (int) ((BIAS_Y - 396) * scale + offsetY), (int) (380 * scale), (int) (379 * scale));
        }

        // 要塞壁
        drawWalls(g2, varMap);

        // 轨迹
        for (Trace trace : traces) {
            if (trace.visible) drawTrace(g2, trace, varMap);
        }

        // 缩放比例（右下角）
        String zoomText = (int) (scale * 100) + "%";
        g2.setFont(new Font("黑体", Font.PLAIN, 12));
        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(zoomText);
        int textHeight = fm.getHeight();
        g2.setColor(Main.DARK_MODE ? new Color(0xAA, 0xAA, 0xAA) : new Color(0x66, 0x66, 0x66));
        g2.drawString(zoomText, w - textWidth - 8, h - textHeight + fm.getAscent() - 8);

        g2.dispose();
    }

    private void drawWalls(Graphics2D g2, Map<String, Integer> varMap){
        for (TraceWall wall : walls) {
            if (!wall.visible) continue;
            int wx = wall.evalX(varMap);
            int wy = wall.evalY(varMap);
            g2.setColor(wall.color);
            g2.setStroke(new BasicStroke(1.2f));
            if (wall.isCore) {
                double x = (BIAS_X + 138 - wx) * scale + offsetX;
                double y = (BIAS_Y - 342 + wy) * scale + offsetY;
                g2.drawOval((int) (x - 53.5 * scale), (int) (y - 53.5 * scale), (int) (26 * scale), (int) (26 * scale));
                g2.drawOval((int) (x - 53.5 * scale), (int) (y + 26.5 * scale), (int) (26 * scale), (int) (26 * scale));
                g2.drawOval((int) (x + 26.5 * scale), (int) (y - 53.5 * scale), (int) (26 * scale), (int) (26 * scale));
                g2.drawOval((int) (x + 26.5 * scale), (int) (y + 26.5 * scale), (int) (26 * scale), (int) (26 * scale));
                g2.drawOval((int) (x - 33.5 * scale), (int) (y - 33.5 * scale), (int) (67 * scale), (int) (67 * scale));
                double l = 36.5 * scale;
                double s = 28 * scale;
                double b = 9.221316 * scale;
                g2.drawLine((int) (x - l), (int) (y - l + b), (int) (x - s), (int) (y - s + b));
                g2.drawLine((int) (x - l + b), (int) (y - l), (int) (x - s + b), (int) (y - s));
                g2.drawLine((int) (x + l), (int) (y - l + b), (int) (x + s), (int) (y - s + b));
                g2.drawLine((int) (x + l - b), (int) (y - l), (int) (x + s - b), (int) (y - s));
                g2.drawLine((int) (x - l), (int) (y + l - b), (int) (x - s), (int) (y + s - b));
                g2.drawLine((int) (x - l + b), (int) (y + l), (int) (x - s + b), (int) (y + s));
                g2.drawLine((int) (x + l), (int) (y + l - b), (int) (x + s), (int) (y + s - b));
                g2.drawLine((int) (x + l - b), (int) (y + l), (int) (x + s - b), (int) (y + s));
                continue;
            }
            double rx = BIAS_X + 174 - wx - 16.85;
            double ry = BIAS_Y - 380 + wy - 17.5;
            double rw = 34.35;
            double rh = 35.0;
            int cx = (int) (rx * scale + offsetX);
            int cy = (int) (ry * scale + offsetY);
            int cw = (int) (rw * scale);
            int ch = (int) (rh * scale);
            int arc = (int) (8 * scale);
            g2.drawRoundRect(cx, cy, cw, ch, arc, arc);
        }
    }

    private void drawTrace(Graphics2D g2, Trace trace, Map<String, Integer> varMap) {
        if (Main.formulaTable == null || !Main.formulaTable.isLoaded()) return;
        int sp1 = trace.evalSpeed0(varMap);
        int sp2 = trace.evalSpeed1(varMap);
        int tx = trace.evalX(varMap);
        int ty = trace.evalY(varMap);
        int twx = trace.evalWallX(varMap);
        int ttimes = trace.evalTimes(varMap);

        int t;
        if (trace.isNear) {
            t = Main.formulaTable.findNearTriggerTime(sp1, sp2, ttimes) + (trace.isBallFirst ? 1 : 0);
            if (t <= 0) return;
        } else {
            t = 884 - twx + 900 * (ttimes - 1) + (trace.isBallFirst ? 1 : 0);
        }

        double[] pos = Main.formulaTable.evaluate(sp1, sp2, t);
        double x0 = pos[FormulaTable.DIM_P1_X];
        double y0 = pos[FormulaTable.DIM_P1_Y];

        double x = x0 - 174 + tx;
        double y = y0 - 380 + ty;
        double xs = trace.isNear ? 4 : 15;
        double ys = trace.isNear ? -8 : -4;

        ArrayList<Point2D.Double> points = new ArrayList<>();

        while (y < 566) {
            t++;
            ys += 0.32;
            x += xs;
            y += ys;
            x = (int) (20 * x) * 0.05;
            y = (int) (20 * y) * 0.05;

            pos = Main.formulaTable.evaluate(sp1, sp2, t);
            double rx = x - pos[FormulaTable.DIM_P2_X];
            double ry = y - pos[FormulaTable.DIM_P2_Y];

            double cx = (rx + BIAS_X) * scale + offsetX;
            double cy = (ry + BIAS_Y) * scale + offsetY;
            points.add(new Point2D.Double(cx, cy));
        }

        if (points.isEmpty()) return;

        Color base = trace.color;

        if (showCenter) {
            renderTrajectory(g2, points, base, 0, 0);
        }
        if (showLandingDetection) {
            renderTrajectory(g2, points, base, 0, 16 * scale);
        }
        if (showWallDetection) {
            renderTrajectory(g2, points, base, 16 * scale, 0);
        }
        if (showUnitOutline) {
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 70));
            g2.setStroke(new BasicStroke(1f));
            double r = 15.5 * scale;
            double d = r * 2;
            for (Point2D.Double p : points) {
                g2.drawOval((int) (p.x - r), (int) (p.y - r), (int) d, (int) d);
            }
        }
    }

    private void renderTrajectory(Graphics2D g2, ArrayList<Point2D.Double> points, Color base, double dx, double dy) {
        g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 80));
        g2.setStroke(new BasicStroke(1.5f));
        for (int i = 1; i < points.size(); i++) {
            Point2D.Double a = points.get(i - 1);
            Point2D.Double b = points.get(i);
            g2.drawLine((int) (a.x + dx), (int) (a.y + dy), (int) (b.x + dx), (int) (b.y + dy));
        }

        g2.setColor(base);
        for (Point2D.Double p : points) {
            g2.fillOval((int) (p.x + dx) - 2, (int) (p.y + dy) - 2, 5, 5);
        }
    }
}
