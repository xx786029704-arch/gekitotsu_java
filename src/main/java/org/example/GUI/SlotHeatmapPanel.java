package org.example.GUI;

import org.example.BattlefieldAnalyzer;
import org.example.I18n;
import org.example.Main;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 阵型受创视图：左侧为阵型图（仅贴图），右侧为热力图（仅指标叠加），两幅都带
 * 阵型工作台坐标系（横轴在上方与纵轴共用左上原点）。
 *
 * <p>坐标系原点取「坐标为 (0,0) 的兵玉的绘制位置」：贴图按 FortPreviewPanel 的锚点与偏置
 * 绘制，因此原点相对 bg.png 左上角偏移 (36,36)。视图范围按数据范围对称外扩：
 * x ∈ [−36, 384]（348+36），y ∈ [−36, 385]（349+36）；画布（bg.png 420×470）按此范围
 * 绘制，底部车板的下缘与轮子被裁掉。刻度仍标注 0/50/…/300 与数据上限 348（纵轴 349）。
 * 车板随 bg.png 一并绘制。
 *
 * <p>指标：受创（承受伤害）/ 阵亡；呈现：气泡点（颜色=强度、大小=比例）或
 * 平滑场（颜色=局部平均指标、透明度=叠加密度，见 {@link #rebuildField()}）。
 * 贴图图层顺序与阵型工作台一致：核心与要塞壁在下、兵玉在上。核心不参与悬停提示与统计。
 */
public class SlotHeatmapPanel extends JPanel {

    /** 槽位指标。 */
    public enum Metric { DAMAGE, DEATHS }

    /** 呈现方式。 */
    public enum Render { BUBBLE, SMOOTH }

    // 编辑器有效数据范围（兵玉 0..348 × 0..349）与贴图锚点（与 FortPreviewPanel 保持一致）
    private static final int EDITOR_W = 348;
    private static final int EDITOR_H = 349;
    private static final int ANCHOR_X = 43;
    private static final int ANCHOR_Y = 55;
    private static final int BIAS_X = 36;
    private static final int BIAS_Y = 36;
    private static final int ANCHOR_CORE_X = 60;
    private static final int ANCHOR_CORE_Y = 60;
    private static final int BIAS_CORE_X = 72;
    private static final int BIAS_CORE_Y = 74;

    // 坐标系原点相对画布左上角的偏移（= 兵玉绘制偏置），视图范围 = 整张画布
    private static final int VIEW_MIN_X = -BIAS_X;
    private static final int VIEW_MIN_Y = -BIAS_Y;

    // 坐标系边距（横轴刻度在上方；右侧留给竖向色带与数值）
    private static final int MARGIN_LEFT = 30;
    private static final int MARGIN_RIGHT = 60;
    private static final int MARGIN_TOP = 20;
    private static final int MARGIN_BOTTOM = 16;

    /** 平滑场半分辨率。 */
    private static final int CELL = 2;
    private static final float SIGMA = 9f;
    private static final int RADIUS = (int) Math.ceil(SIGMA * 3);

    private List<BattlefieldAnalyzer.SlotMeta> metas = List.of();
    private double[] damage = new double[0];
    private double[] deaths = new double[0];
    private Metric metric = Metric.DAMAGE;
    private Render render = Render.BUBBLE;

    private final BufferedImage bgImage;
    private final int viewMaxX;
    private final int viewMaxY;
    private final int fieldW;
    private final int fieldH;
    private final Map<Integer, BufferedImage> spriteCache = new HashMap<>();
    /** 平滑场：核加权指标和（分子）与核密度和（分母）；颜色=局部平均，透明度=叠加密度。 */
    private float[] fieldWeight;
    private float[] fieldDensity;
    private double fieldMaxDensity = 1;
    private boolean fieldDirty = true;

    private final FormationView formationView = new FormationView();
    private final HeatView heatView = new HeatView();

    public SlotHeatmapPanel() {
        super(new GridLayout(1, 2, 6, 6));
        add(formationView);
        add(heatView);
        bgImage = loadImage("/Bg/bg.png");
        int canvasW = bgImage != null ? bgImage.getWidth() : EDITOR_W + 2 * BIAS_X;
        int canvasH = bgImage != null ? bgImage.getHeight() : EDITOR_H + 2 * BIAS_Y;
        // 视图范围按 348+a / 349+b 外扩；画布更高时在 349+36 处截断（车板底部与轮子被裁掉）
        int visibleW = Math.min(canvasW, EDITOR_W + 2 * BIAS_X);
        int visibleH = Math.min(canvasH, EDITOR_H + 2 * BIAS_Y);
        viewMaxX = visibleW - BIAS_X;
        viewMaxY = visibleH - BIAS_Y;
        fieldW = (visibleW + CELL - 1) / CELL;
        fieldH = (visibleH + CELL - 1) / CELL;
    }

    void setData(List<BattlefieldAnalyzer.SlotMeta> metas,
                 double[] damage, double[] deaths) {
        this.metas = metas == null ? List.of() : metas;
        this.damage = damage == null ? new double[0] : damage;
        this.deaths = deaths == null ? new double[0] : deaths;
        fieldDirty = true;
        formationView.repaint();
        heatView.repaint();
    }

    void setMetric(Metric metric) {
        this.metric = metric;
        fieldDirty = true;
        heatView.repaint();
    }

    void setRender(Render render) {
        this.render = render;
        heatView.repaint();
    }

    void updateDarkMode() {
        formationView.repaint();
        heatView.repaint();
    }

    // ===== 共享计算 =====

    private double valueAt(int index) {
        double[] values = switch (metric) {
            case DAMAGE -> damage;
            case DEATHS -> deaths;
        };
        return index >= 0 && index < values.length ? values[index] : 0;
    }

    private double maxValue() {
        double max = 0;
        for (int i = 0; i < metas.size(); i++) {
            max = Math.max(max, valueAt(i));
        }
        return max;
    }

    /** 叠加层中心：与贴图锚点一致（兵玉 +36/+36，核心 +72/+74）。 */
    private static double centerX(BattlefieldAnalyzer.SlotMeta meta) {
        return meta.x() + (meta.core() ? BIAS_CORE_X : BIAS_X);
    }

    private static double centerY(BattlefieldAnalyzer.SlotMeta meta) {
        return meta.y() + (meta.core() ? BIAS_CORE_Y : BIAS_Y);
    }

    /** 顺序色带：0 → 浅黄，0.5 → 橙，1 → 红。 */
    private static Color heatColor(double t) {
        Color low = new Color(0xFFE082);
        Color mid = new Color(0xFB8C00);
        Color high = new Color(0xD32F2F);
        t = Math.max(0, Math.min(1, t));
        if (t < 0.5) {
            return lerp(low, mid, t * 2);
        }
        return lerp(mid, high, (t - 0.5) * 2);
    }

    private static Color lerp(Color a, Color b, double t) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /**
     * 构建平滑场：每个槽位按其指标值贡献高斯核。同时累加两路数据——
     * 核加权指标和（用于颜色 = 局部平均指标）与核密度和（用于透明度 = 叠加密度），
     * 避免「单位密集的低平均承伤」被累加误判为「单位稀疏的高平均承伤」。
     */
    private void rebuildField() {
        fieldWeight = new float[fieldW * fieldH];
        fieldDensity = new float[fieldW * fieldH];
        fieldMaxDensity = 1;
        fieldDirty = false;
        if (maxValue() <= 0 || metas.isEmpty()) {
            return;
        }
        // 加窗高斯核：减去半径处的权重，使核在边缘处连续归零（避免出现硬边界）
        double edge = Math.exp(-(RADIUS * RADIUS) / (2.0 * SIGMA * SIGMA));
        float[] kernel = new float[RADIUS + 1];
        for (int i = 0; i <= RADIUS; i++) {
            kernel[i] = (float) ((Math.exp(-(i * i) / (2.0 * SIGMA * SIGMA)) - edge) / (1.0 - edge));
        }
        for (int i = 0; i < metas.size(); i++) {
            double value = valueAt(i);
            if (value <= 0) {
                continue;
            }
            int cx = Math.max(0, Math.min(fieldW - 1, (int) centerX(metas.get(i)) / CELL));
            int cy = Math.max(0, Math.min(fieldH - 1, (int) centerY(metas.get(i)) / CELL));
            for (int dy = -RADIUS; dy <= RADIUS; dy++) {
                int py = cy + dy;
                if (py < 0 || py >= fieldH) {
                    continue;
                }
                float ky = kernel[Math.abs(dy)];
                for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                    int px = cx + dx;
                    if (px < 0 || px >= fieldW) {
                        continue;
                    }
                    float k = kernel[Math.abs(dx)] * ky;
                    fieldWeight[py * fieldW + px] += (float) value * k;
                    fieldDensity[py * fieldW + px] += k;
                }
            }
        }
        double maxDensity = 0;
        for (float d : fieldDensity) {
            maxDensity = Math.max(maxDensity, d);
        }
        fieldMaxDensity = maxDensity > 0 ? maxDensity : 1;
    }

    // ===== 坐标系 =====

    private record View(Rectangle rect, double scale) {}

    /** 视图范围 = 数据范围外扩（画布超出部分裁掉）；坐标 = 画布像素 − 绘制偏置，原点即 (0,0) 兵玉的绘制位置。 */
    private View computeView(int w, int h) {
        double spanX = viewMaxX - VIEW_MIN_X;
        double spanY = viewMaxY - VIEW_MIN_Y;
        double scale = Math.min((w - MARGIN_LEFT - MARGIN_RIGHT) / spanX,
                (h - MARGIN_TOP - MARGIN_BOTTOM) / spanY);
        if (scale <= 0) {
            return null;
        }
        int vw = (int) Math.round(spanX * scale);
        int vh = (int) Math.round(spanY * scale);
        int vx = MARGIN_LEFT + (w - MARGIN_LEFT - MARGIN_RIGHT - vw) / 2;
        int vy = MARGIN_TOP + (h - MARGIN_TOP - MARGIN_BOTTOM - vh) / 2;
        return new View(new Rectangle(vx, vy, vw, vh), scale);
    }

    /** 纵/横轴值 → 屏幕像素。 */
    private static int screenX(View view, double value) {
        return view.rect().x + (int) Math.round((value - VIEW_MIN_X) * view.scale());
    }

    private static int screenY(View view, double value) {
        return view.rect().y + (int) Math.round((value - VIEW_MIN_Y) * view.scale());
    }

    /** 每 50 坐标的网格（含 0 处；画布外扩区域无负刻度线）。 */
    private void drawGrid(Graphics2D g2, View view) {
        Rectangle r = view.rect();
        g2.setColor(MatchupCharts.gridColor());
        for (int v = 0; v <= viewMaxX; v += 50) {
            int p = screenX(view, v);
            g2.drawLine(p, r.y, p, r.y + r.height);
        }
        for (int v = 0; v <= viewMaxY; v += 50) {
            int p = screenY(view, v);
            g2.drawLine(r.x, p, r.x + r.width, p);
        }
    }

    /** 边框与刻度：横轴在上方（与纵轴共用左上原点），标注 0/50/…/300 与数据上限 348（纵轴 349）。 */
    private void drawFrameAndTicks(Graphics2D g2, View view) {
        Rectangle r = view.rect();
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(MatchupCharts.textColor());
        g2.drawRect(r.x, r.y, r.width, r.height);
        for (int v = 0; v <= 300; v += 50) {
            String label = String.valueOf(v);
            int lx = v == 0 ? screenX(view, v) + 2
                    : screenX(view, v) - fm.stringWidth(label) / 2;
            g2.drawString(label, lx, r.y - 4);
        }
        String maxX = String.valueOf(EDITOR_W);
        g2.drawString(maxX, screenX(view, EDITOR_W) - fm.stringWidth(maxX) / 2, r.y - 4);
        for (int v = 0; v <= 300; v += 50) {
            String label = String.valueOf(v);
            g2.drawString(label, r.x - 4 - fm.stringWidth(label),
                    screenY(view, v) + fm.getAscent() / 2 - 1);
        }
        String maxY = String.valueOf(EDITOR_H);
        g2.drawString(maxY, r.x - 4 - fm.stringWidth(maxY),
                screenY(view, EDITOR_H) + fm.getAscent() / 2 - 1);
    }

    private static String slotName(BattlefieldAnalyzer.SlotMeta meta) {
        return meta.core() ? I18n.t("battlefield.slot.core") : I18n.unitName(meta.type());
    }

    /** 最近的槽位（按叠加层中心），超过阈值返回 -1；核心不参与提示。 */
    private int nearestSlot(Point p, View view, double radiusPx) {
        int best = -1;
        double bestDist = radiusPx * radiusPx;
        Rectangle r = view.rect();
        for (int i = 0; i < metas.size(); i++) {
            if (metas.get(i).core()) {
                continue;
            }
            double cx = r.x + centerX(metas.get(i)) * view.scale();
            double cy = r.y + centerY(metas.get(i)) * view.scale();
            double d = (p.x - cx) * (p.x - cx) + (p.y - cy) * (p.y - cy);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        return best;
    }

    /** 左幅：仅阵型图（贴图图层顺序与工作台一致）。 */
    private final class FormationView extends JPanel {
        FormationView() {
            setToolTipText("");
            ToolTipManager.sharedInstance().registerComponent(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int w = getWidth();
            int h = getHeight();
            View view = w < 80 || h < 80 ? null : computeView(w, h);
            if (view == null) {
                g2.dispose();
                return;
            }
            Rectangle r = view.rect();
            g2.setClip(r);
            if (bgImage == null) {
                g2.setColor(Main.DARK_MODE ? new Color(0x263238) : new Color(0xECEFF1));
                g2.fillRect(r.x, r.y, r.width, r.height);
            }
            drawGrid(g2, view);
            if (bgImage != null) {
                // 按可见范围绘制画布（底部超出 349+36 的车板部分被裁掉）；车板盖住网格
                g2.drawImage(bgImage, r.x, r.y, r.x + r.width, r.y + r.height,
                        0, 0, Math.min(bgImage.getWidth(), viewMaxX + BIAS_X),
                        Math.min(bgImage.getHeight(), viewMaxY + BIAS_Y), null);
            }
            // 图层顺序与阵型工作台一致：核心与要塞壁在下（核心先画），兵玉在上
            for (BattlefieldAnalyzer.SlotMeta meta : metas) {
                if (meta.core()) {
                    drawSprite(g2, view, meta);
                }
            }
            for (BattlefieldAnalyzer.SlotMeta meta : metas) {
                if (!meta.core() && Unit.isWallLike(meta.type())) {
                    drawSprite(g2, view, meta);
                }
            }
            for (BattlefieldAnalyzer.SlotMeta meta : metas) {
                if (!meta.core() && !Unit.isWallLike(meta.type())) {
                    drawSprite(g2, view, meta);
                }
            }
            g2.setClip(null);
            drawFrameAndTicks(g2, view);
            g2.dispose();
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            int w = getWidth();
            int h = getHeight();
            View view = computeView(w, h);
            if (view == null) {
                return null;
            }
            int index = nearestSlot(event.getPoint(), view, 22);
            if (index < 0) {
                return null;
            }
            BattlefieldAnalyzer.SlotMeta meta = metas.get(index);
            return "<html>" + I18n.t("battlefield.slot.tip.head",
                    slotName(meta), meta.x(), meta.y()) + "</html>";
        }
    }

    /** 右幅：仅热力图（气泡或平滑场）。 */
    private final class HeatView extends JPanel {
        HeatView() {
            setToolTipText("");
            ToolTipManager.sharedInstance().registerComponent(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int w = getWidth();
            int h = getHeight();
            View view = w < 80 || h < 80 ? null : computeView(w, h);
            if (view == null) {
                g2.dispose();
                return;
            }
            Rectangle r = view.rect();
            g2.setColor(Main.DARK_MODE ? new Color(0x263238) : new Color(0xECEFF1));
            g2.fillRect(r.x, r.y, r.width, r.height);
            drawGrid(g2, view);

            double max = maxValue();
            if (max > 0) {
                g2.setClip(r);
                if (render == Render.SMOOTH) {
                    if (fieldDirty) {
                        rebuildField();
                    }
                    drawSmoothField(g2, view);
                } else {
                    drawBubbles(g2, view, max);
                }
                g2.setClip(null);
            }
            drawFrameAndTicks(g2, view);
            drawLegend(g2, view, max);
            g2.dispose();
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            int w = getWidth();
            int h = getHeight();
            View view = computeView(w, h);
            if (view == null) {
                return null;
            }
            int index = nearestSlot(event.getPoint(), view, 22);
            if (index < 0) {
                return null;
            }
            BattlefieldAnalyzer.SlotMeta meta = metas.get(index);
            return "<html>"
                    + I18n.t("battlefield.slot.tip.head", slotName(meta), meta.x(), meta.y())
                    + "<br>" + I18n.t("battlefield.slot.tip.values",
                            fmt(damage, index), fmt(deaths, index))
                    + "</html>";
        }
    }

    // ===== 绘制 =====

    private void drawSprite(Graphics2D g2, View view, BattlefieldAnalyzer.SlotMeta meta) {
        BufferedImage sprite = spriteCache.get(meta.type());
        if (sprite == null) {
            sprite = loadImage(String.format("/Units/u%04d.png", meta.type()));
            if (sprite == null) {
                return;
            }
            spriteCache.put(meta.type(), sprite);
        }
        Graphics2D ug = (Graphics2D) g2.create();
        try {
            ug.translate(view.rect().x, view.rect().y);
            ug.scale(view.scale(), view.scale());
            double mapX = meta.x();
            double mapY = meta.y();
            if (meta.core()) {
                ug.translate(mapX - ANCHOR_CORE_X + BIAS_CORE_X, mapY - ANCHOR_CORE_Y + BIAS_CORE_Y);
            } else {
                ug.translate(mapX + BIAS_X, mapY + BIAS_Y);
                ug.rotate(Math.toRadians(meta.r()));
                if (meta.r() >= 90 && meta.r() <= 270) {
                    ug.scale(1, -1);
                }
                ug.translate(-ANCHOR_X, -ANCHOR_Y);
            }
            ug.drawImage(sprite, 0, 0, null);
        } finally {
            ug.dispose();
        }
    }

    private void drawBubbles(Graphics2D g2, View view, double max) {
        Rectangle r = view.rect();
        for (int i = 0; i < metas.size(); i++) {
            double value = valueAt(i);
            if (value <= 0) {
                continue;
            }
            double t = Math.min(1, value / max);
            int cx = r.x + (int) Math.round(centerX(metas.get(i)) * view.scale());
            int cy = r.y + (int) Math.round(centerY(metas.get(i)) * view.scale());
            int radius = 4 + (int) Math.round(14 * Math.sqrt(t));
            Color color = heatColor(t);
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    (int) Math.round(110 + 110 * t)));
            g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
            g2.setColor(new Color(0, 0, 0, 130));
            g2.setStroke(new BasicStroke(1.3f));
            g2.drawOval(cx - radius, cy - radius, radius * 2, radius * 2);
        }
    }

    private void drawSmoothField(Graphics2D g2, View view) {
        BufferedImage image = new BufferedImage(fieldW, fieldH, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[fieldW * fieldH];
        double maxSlot = maxValue();
        for (int i = 0; i < pixels.length; i++) {
            double density = fieldDensity[i];
            if (density <= 0) {
                continue;
            }
            // 颜色 = 局部（核加权）平均指标，按单槽上限归一：密集低伤保持低色温、不因叠加偏红
            double average = fieldWeight[i] / density;
            double tc = maxSlot > 0 ? Math.min(1.0, average / maxSlot) : 0;
            // 透明度 = 叠加密度（sqrt 映射，边缘连续淡出）
            double ta = Math.min(1.0, density / fieldMaxDensity);
            int alpha = (int) Math.round(255 * Math.pow(ta, 0.5));
            if (alpha <= 0) {
                continue;
            }
            Color color = heatColor(tc);
            pixels[i] = (alpha << 24) | (color.getRGB() & 0xFFFFFF);
        }
        image.setRGB(0, 0, fieldW, fieldH, pixels, 0, fieldW);
        g2.drawImage(image, view.rect().x, view.rect().y, view.rect().width, view.rect().height, null);
    }

    /** 竖向色带：绘制在绘图区右侧的边距内（顶=最大值，底=0），不遮挡热力图。 */
    private void drawLegend(Graphics2D g2, View view, double max) {
        Rectangle r = view.rect();
        g2.setFont(I18n.font(Font.PLAIN, 10));
        FontMetrics fm = g2.getFontMetrics();
        int barW = 9;
        int barH = Math.max(60, Math.min(160, r.height / 2));
        int labelW = Math.max(fm.stringWidth("000.0k"), fm.stringWidth("0000"));
        int barX = r.x + r.width + 6 + labelW + 4;
        int top = r.y + (r.height - barH) / 2;
        for (int i = 0; i < barH; i++) {
            double t = 1.0 - i / (double) (barH - 1);
            g2.setColor(heatColor(t));
            g2.fillRect(barX, top + i, barW, 1);
        }
        g2.setColor(MatchupCharts.gridColor());
        g2.drawRect(barX, top, barW, barH);
        g2.setColor(MatchupCharts.textColor());
        String maxLabel = max <= 0 ? "—" : compact(max);
        g2.drawString(maxLabel, barX - 4 - fm.stringWidth(maxLabel), top + fm.getAscent() / 2);
        g2.drawString("0", barX - 4 - fm.stringWidth("0"), top + barH + fm.getAscent() / 2);
    }

    /** 紧凑数值：≥1000 用 k（保留一位小数），否则千分位整数。 */
    private static String compact(double value) {
        if (value >= 1000) {
            return String.format("%.1fk", value / 1000.0);
        }
        return String.format("%,.0f", value);
    }

    private static String fmt(double[] values, int index) {
        return index < values.length ? String.format("%,.0f", values[index]) : "—";
    }

    private static BufferedImage loadImage(String path) {
        try (InputStream in = SlotHeatmapPanel.class.getResourceAsStream(path)) {
            if (in == null) {
                return null;
            }
            return ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }
}
