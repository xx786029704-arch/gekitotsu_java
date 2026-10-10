package org.example.GUI;

import org.example.I18n;
import org.example.Main;
import org.example.MatchupAnalyzer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 对手核心位置胜率热力图（平滑版）。
 *
 * <p>坐标采用阵型工作台标准：核心 x/y ∈ 0..276（= 原始码坐标 − 52/−58）。
 * 每个测试阵型在核心位置贡献一个高斯核，形成连续的胜率场（红-黄-绿渐变，估计值），
 * 透明度随样本密度增加；真实样本点额外用小圆点标记（绿=我方胜、红=我方负、灰=平/超时）。
 */
public class CoreHeatmapPanel extends JPanel {

    /** 工作台核心坐标上限（x、y 同域）。 */
    private static final int FIELD = 276;
    /** 平滑场累加分辨率（半分辨率，绘制时双线性放大）。 */
    private static final int CELLS = FIELD / 2;
    private static final int MAX_TIP_NAMES = 8;

    private List<MatchupAnalyzer.FortMeta> metas = List.of();
    private int[] statuses = new int[0];
    private int bandwidth = 20;
    private float[] weight = new float[CELLS * CELLS];
    private float[] value = new float[CELLS * CELLS];
    private Rectangle view;
    private double scale;

    public CoreHeatmapPanel() {
        setToolTipText("");
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    void setData(List<MatchupAnalyzer.FortMeta> metas, int[] statuses) {
        this.metas = metas == null ? List.of() : metas;
        this.statuses = statuses == null ? new int[0] : statuses;
        rebuild();
        repaint();
    }

    /** 平滑半径（工作台坐标像素）。 */
    void setBandwidth(int bandwidth) {
        int value = Math.max(4, bandwidth);
        if (value == this.bandwidth) {
            return;
        }
        this.bandwidth = value;
        rebuild();
        repaint();
    }

    void updateDarkMode() {
        repaint();
    }

    /** 逐样本高斯核累加（可分离模糊），得到胜率场与密度场。 */
    private void rebuild() {
        weight = new float[CELLS * CELLS];
        value = new float[CELLS * CELLS];
        if (metas.isEmpty()) {
            return;
        }
        // 冲激：每个样本在其核心格累加权重与 权重×胜率（胜=100、负=0、平=50；超时/异常不参与估计）
        for (int i = 0; i < metas.size(); i++) {
            MatchupAnalyzer.FortMeta meta = metas.get(i);
            int status = i < statuses.length ? statuses[i] : -2;
            double rate;
            if (status == 1) {
                rate = 100;
            } else if (status == 2) {
                rate = 0;
            } else if (status == 0) {
                rate = 50;
            } else {
                continue;
            }
            int cx = Math.max(0, Math.min(CELLS - 1, Math.round(meta.coreX() / 2f)));
            int cy = Math.max(0, Math.min(CELLS - 1, Math.round(meta.coreY() / 2f)));
            weight[cy * CELLS + cx] += 1f;
            value[cy * CELLS + cx] += (float) rate;
        }
        // 分离高斯模糊：σ 与半径按半分辨率换算
        float sigma = Math.max(1f, bandwidth / 2f);
        int radius = Math.max(1, (int) Math.ceil(sigma * 3));
        float[] kernel = new float[radius + 1];
        for (int i = 0; i <= radius; i++) {
            kernel[i] = (float) Math.exp(-(i * i) / (2.0 * sigma * sigma));
        }
        weight = blur(weight, kernel, radius);
        value = blur(value, kernel, radius);
    }

    private static float[] blur(float[] src, float[] kernel, int radius) {
        float[] horizontal = new float[src.length];
        for (int y = 0; y < CELLS; y++) {
            int row = y * CELLS;
            for (int x = 0; x < CELLS; x++) {
                float sum = 0;
                for (int k = -radius; k <= radius; k++) {
                    int sx = Math.max(0, Math.min(CELLS - 1, x + k));
                    sum += src[row + sx] * kernel[Math.abs(k)];
                }
                horizontal[row + x] = sum;
            }
        }
        float[] out = new float[src.length];
        for (int y = 0; y < CELLS; y++) {
            for (int x = 0; x < CELLS; x++) {
                float sum = 0;
                for (int k = -radius; k <= radius; k++) {
                    int sy = Math.max(0, Math.min(CELLS - 1, y + k));
                    sum += horizontal[sy * CELLS + x] * kernel[Math.abs(k)];
                }
                out[y * CELLS + x] = sum;
            }
        }
        return out;
    }

    /** 生成胜率场图像：颜色由估计胜率决定，透明度随样本密度增加。 */
    private BufferedImage buildFieldImage() {
        BufferedImage image = new BufferedImage(CELLS, CELLS, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[CELLS * CELLS];
        for (int i = 0; i < pixels.length; i++) {
            float w = weight[i];
            if (w < 1e-3f) {
                continue;
            }
            double rate = value[i] / w;
            Color base = MatchupCharts.rateColorOpaque(rate);
            int alpha = (int) Math.round(255 * Math.min(1.0, 0.38 + 0.62 * (w / 3.0)));
            pixels[i] = (alpha << 24) | (base.getRGB() & 0xFFFFFF);
        }
        image.setRGB(0, 0, CELLS, CELLS, pixels, 0, CELLS);
        return image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int w = getWidth();
        int h = getHeight();
        if (w < 80 || h < 80) {
            g2.dispose();
            return;
        }

        int marginLeft = 30;
        int marginRight = 26;  // 给右上角外置的最大值标注留位置
        int marginTop = 22;    // 横轴刻度在图像上方，与纵轴共用左上原点
        int marginBottom = 42; // 图例在图像下方
        scale = Math.min((w - marginLeft - marginRight) / (double) FIELD,
                (h - marginTop - marginBottom) / (double) FIELD);
        if (scale <= 0) {
            g2.dispose();
            return;
        }
        int fieldSize = (int) Math.round(FIELD * scale);
        int fx = marginLeft + (w - marginLeft - marginRight - fieldSize) / 2;
        int fy = marginTop + (h - marginTop - marginBottom - fieldSize) / 2;
        view = new Rectangle(fx, fy, fieldSize, fieldSize);

        g2.setFont(I18n.font(Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();

        // 场地底色与每 50 坐标的网格
        g2.setColor(Main.DARK_MODE ? new Color(0x263238) : new Color(0xECEFF1));
        g2.fillRect(fx, fy, fieldSize, fieldSize);
        g2.setColor(MatchupCharts.gridColor());
        for (int v = 50; v < FIELD; v += 50) {
            int p = (int) Math.round(v * scale);
            g2.drawLine(fx + p, fy, fx + p, fy + fieldSize);
            g2.drawLine(fx, fy + p, fx + fieldSize, fy + p);
        }

        // 平滑胜率场
        if (!metas.isEmpty()) {
            BufferedImage fieldImage = buildFieldImage();
            g2.drawImage(fieldImage, fx, fy, fieldSize, fieldSize, null);
        }

        // 真实样本点标记（结果为绿/红/灰，深色描边保证对比）
        for (int i = 0; i < metas.size(); i++) {
            MatchupAnalyzer.FortMeta meta = metas.get(i);
            int status = i < statuses.length ? statuses[i] : -2;
            int px = fx + (int) Math.round(Math.max(0, Math.min(FIELD, meta.coreX())) * scale);
            int py = fy + (int) Math.round(Math.max(0, Math.min(FIELD, meta.coreY())) * scale);
            Color fill = switch (status) {
                case 1 -> MatchupCharts.winColor();
                case 2 -> MatchupCharts.loseColor();
                default -> MatchupCharts.neutralColor();
            };
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillOval(px - 3, py - 3, 6, 6);
            g2.setColor(fill);
            g2.fillOval(px - 2, py - 2, 4, 4);
        }

        // 边框与坐标刻度：横轴在图像上方（与纵轴共用左上原点），最大值 276 额外标注
        g2.setColor(MatchupCharts.textColor());
        g2.drawRect(fx, fy, fieldSize, fieldSize);
        String maxLabel = String.valueOf(FIELD);
        for (int v = 0; v < FIELD; v += 50) {
            String label = String.valueOf(v);
            int p = (int) Math.round(v * scale);
            int lx = v == 0 ? fx + 2 : fx + p - fm.stringWidth(label) / 2;
            g2.drawString(label, lx, fy - 4);
            g2.drawString(label, fx - 4 - fm.stringWidth(label), fy + p + fm.getAscent() / 2 - 1);
        }
        g2.drawString(maxLabel, fx + fieldSize + 2, fy - 4);
        g2.drawString(maxLabel, fx - 4 - fm.stringWidth(maxLabel),
                fy + fieldSize + fm.getAscent() / 2 - 1);

        drawLegend(g2, w);
        g2.dispose();
    }

    /** 图像下方图例：红-黄-绿胜率色带 + 0/50/100% 标注。 */
    private void drawLegend(Graphics2D g2, int width) {
        g2.setFont(I18n.font(Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        int barW = 90;
        int barH = 8;
        int x = width - 10 - barW;
        int y = getHeight() - (fm.getAscent() + 2 + barH + fm.getAscent()) - 4;
        g2.setColor(MatchupCharts.textColor());
        g2.drawString(I18n.t("matchup.heatmap.rate"), x, y + fm.getAscent());
        int barY = y + fm.getAscent() + 2;
        for (int i = 0; i < barW; i++) {
            g2.setColor(MatchupCharts.rateColorOpaque(i * 100.0 / (barW - 1)));
            g2.fillRect(x + i, barY, 1, barH);
        }
        g2.setColor(MatchupCharts.textColor());
        int labelY = barY + barH + fm.getAscent();
        g2.drawString("0%", x, labelY);
        String hundred = "100%";
        g2.drawString(hundred, x + barW - fm.stringWidth(hundred), labelY);
        String fifty = "50%";
        g2.drawString(fifty, x + barW / 2 - fm.stringWidth(fifty) / 2, labelY);
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        if (view == null || scale <= 0) {
            return null;
        }
        Point p = event.getPoint();
        if (!view.contains(p)) {
            return null;
        }
        double fx = (p.x - view.x) / scale;
        double fy = (p.y - view.y) / scale;
        int cx = Math.max(0, Math.min(CELLS - 1, (int) Math.round(fx / 2)));
        int cy = Math.max(0, Math.min(CELLS - 1, (int) Math.round(fy / 2)));
        float w = weight[cy * CELLS + cx];

        List<String> names = new ArrayList<>();
        int nearby = 0;
        double radius = Math.max(1, bandwidth);
        for (int i = 0; i < metas.size(); i++) {
            MatchupAnalyzer.FortMeta meta = metas.get(i);
            double dx = meta.coreX() - fx;
            double dy = meta.coreY() - fy;
            if (dx * dx + dy * dy <= radius * radius) {
                nearby++;
                if (names.size() < MAX_TIP_NAMES) {
                    names.add(meta.name() == null || meta.name().isEmpty()
                            ? I18n.t("common.none") : meta.name());
                }
            }
        }
        if (w < 1e-3f && nearby == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder("<html>");
        sb.append(I18n.t("matchup.heatmap.tip.point",
                (int) Math.round(fx), (int) Math.round(fy)));
        if (w >= 1e-3f) {
            sb.append("<br>").append(I18n.t("matchup.heatmap.tip.estimate",
                    MatchupCharts.rateText(value[cy * CELLS + cx] / w)));
        }
        if (nearby > 0) {
            sb.append("<br>").append(I18n.t("matchup.heatmap.tip.nearby", nearby));
            sb.append("<br>").append(I18n.t("matchup.heatmap.tip.forts", String.join(", ", names)
                    + (nearby > names.size() ? "…" : "")));
        }
        sb.append("</html>");
        return sb.toString();
    }
}
