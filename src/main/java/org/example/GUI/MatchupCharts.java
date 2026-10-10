package org.example.GUI;

import org.example.I18n;
import org.example.Main;
import org.example.MatchupAnalyzer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * 对阵画像的图表与共享配色/分箱工具。
 *
 * <p>「节奏与赢面」页含两幅自绘图表：时长分布与胜率走势（柱状 + 折线双轴）、
 * 赢面（获胜方剩余 HP）分布。「时长分布与胜率走势」左轴为局数、右轴为胜率，
 * 胜率用无端点圆点的简单折线绘制。超时与异常对局不计入图表。
 * 「加速度相性」页为按对手加速度等级的胜率柱状图。
 * 「Keener 修正」页为对手修正胜率散点图（我方修正胜率虚线与实际胜率标记）。
 */
final class MatchupCharts {

    private MatchupCharts() {
    }

    static Color winColor() {
        return Main.DARK_MODE ? new Color(0x81C784) : new Color(0x2E7D32);
    }

    static Color loseColor() {
        return Main.DARK_MODE ? new Color(0xE57373) : new Color(0xC62828);
    }

    static Color neutralColor() {
        return Main.DARK_MODE ? new Color(0x90A4AE) : new Color(0x78909C);
    }

    static Color textColor() {
        return Main.DARK_MODE ? new Color(0xCFD8DC) : new Color(0x37474F);
    }

    static Color gridColor() {
        return Main.DARK_MODE ? new Color(0x37474F) : new Color(0xD5DBE0);
    }

    static Color lineColor() {
        Color accent = UIManager.getColor("Component.accentColor");
        return accent != null ? accent : new Color(0x1E88E5);
    }

    static String rateText(double rate) {
        return Double.isNaN(rate) ? "—" : String.format("%.1f%%", rate);
    }

    /** 热力图/图例用的胜率取色（红 0% → 黄 50% → 绿 100%）。 */
    static Color rateColorOpaque(double rate) {
        double t = Math.max(0, Math.min(1, rate / 100.0));
        Color red = new Color(0xE53935);
        Color yellow = new Color(0xFDD835);
        Color green = new Color(0x43A047);
        return t < 0.5 ? lerp(red, yellow, t * 2) : lerp(yellow, green, (t - 0.5) * 2);
    }

    private static Color lerp(Color a, Color b, double t) {
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** 对局时长分箱（胜/负/平），箱宽按最长对局自适应；超时与异常不计入。 */
    static final class FrameBins {
        final int binWidth;
        final int binCount;
        final int[] win;
        final int[] lose;
        final int[] draw;

        FrameBins(int[] frames, int[] statuses) {
            int n = Math.min(frames.length, statuses.length);
            int max = 0;
            boolean any = false;
            for (int i = 0; i < n; i++) {
                int s = statuses[i];
                if (s == 0 || s == 1 || s == 2) {
                    any = true;
                    max = Math.max(max, frames[i]);
                }
            }
            if (!any || max <= 0) {
                binWidth = 100;
                binCount = 1;
            } else {
                binWidth = Math.max(50, (int) Math.ceil(max / 24.0 / 50.0) * 50);
                binCount = Math.max(1, (max + binWidth - 1) / binWidth);
            }
            win = new int[binCount];
            lose = new int[binCount];
            draw = new int[binCount];
            for (int i = 0; i < n; i++) {
                int s = statuses[i];
                if (s != 0 && s != 1 && s != 2) {
                    continue;
                }
                int b = Math.min(binCount - 1, Math.max(0, frames[i]) / binWidth);
                if (s == 1) {
                    win[b]++;
                } else if (s == 2) {
                    lose[b]++;
                } else {
                    draw[b]++;
                }
            }
        }

        int samples(int bin) {
            return win[bin] + lose[bin] + draw[bin];
        }

        double winRate(int bin) {
            int decisive = win[bin] + lose[bin] + draw[bin];
            return decisive > 0 ? (2.0 * win[bin] + draw[bin]) * 50.0 / decisive : Double.NaN;
        }

        int maxSamples() {
            int max = 1;
            for (int b = 0; b < binCount; b++) {
                max = Math.max(max, samples(b));
            }
            return max;
        }
    }

    /** 图表基类：统一字体、提示注册与坐标轴绘制。 */
    abstract static class ChartPanel extends JPanel {

        ChartPanel() {
            setToolTipText("");
            ToolTipManager.sharedInstance().registerComponent(this);
        }

        protected final void drawTitle(Graphics2D g2, String title) {
            Font old = g2.getFont();
            g2.setFont(I18n.font(Font.BOLD, 12));
            g2.setColor(textColor());
            g2.drawString(title, 6, 14);
            g2.setFont(old);
        }

        /** 画 y 轴网格；valueLabels[i] 对应 i/divisions 处。 */
        protected final void drawYGrid(Graphics2D g2, Rectangle r, int divisions, String[] valueLabels) {
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i <= divisions; i++) {
                int y = r.y + r.height - (int) Math.round(r.height * i / (double) divisions);
                g2.setColor(gridColor());
                g2.drawLine(r.x, y, r.x + r.width, y);
                g2.setColor(textColor());
                String label = valueLabels[i];
                g2.drawString(label, r.x - 4 - fm.stringWidth(label), y + fm.getAscent() / 2 - 1);
            }
        }

        /** 画 x 轴刻度标签；每 step 个箱一个，labelFor 提供文本。 */
        protected final void drawXLabels(Graphics2D g2, Rectangle r, int binCount, int step,
                                         java.util.function.IntFunction<String> labelFor) {
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(textColor());
            double slot = r.width / (double) binCount;
            for (int b = 0; b < binCount; b += step) {
                String label = labelFor.apply(b);
                int x = r.x + (int) Math.round(slot * b + slot / 2) - fm.stringWidth(label) / 2;
                g2.drawString(label, x, r.y + r.height + fm.getAscent() + 2);
            }
        }

        protected final void drawLegend(Graphics2D g2, int right, String[] labels, Color[] colors) {
            drawLegend(g2, right, labels, colors, null);
        }

        /** 图例；lineLabel 非空时在最左侧额外画一段折线色样。 */
        protected final void drawLegend(Graphics2D g2, int right, String[] labels, Color[] colors,
                                        String lineLabel) {
            FontMetrics fm = g2.getFontMetrics();
            int x = right - 6;
            int y = 11;
            for (int i = labels.length - 1; i >= 0; i--) {
                int textWidth = fm.stringWidth(labels[i]);
                x -= textWidth;
                g2.setColor(textColor());
                g2.drawString(labels[i], x, y);
                x -= 5 + 8;
                g2.setColor(colors[i]);
                g2.fillRect(x, y - 8, 8, 8);
                x -= 10;
            }
            if (lineLabel != null) {
                int textWidth = fm.stringWidth(lineLabel);
                x -= textWidth;
                g2.setColor(textColor());
                g2.drawString(lineLabel, x, y);
                x -= 5 + 16;
                g2.setColor(lineColor());
                g2.setStroke(new BasicStroke(1.8f));
                g2.drawLine(x, y - 4, x + 16, y - 4);
            }
        }

        protected static String formatFrames(int frames) {
            return frames >= 1000 ? String.format("%.1fk", frames / 1000.0) : String.valueOf(frames);
        }

        protected static String[] winLoseDrawLegend() {
            return new String[]{I18n.t("matchup.legend.win"), I18n.t("matchup.legend.lose"), I18n.t("matchup.legend.draw")};
        }

        protected static Color[] winLoseDrawColors() {
            return new Color[]{winColor(), loseColor(), neutralColor()};
        }

        protected static String tipHtml(String head, String stats) {
            return "<html>" + head + "<br>" + stats + "</html>";
        }
    }

    /**
     * 时长分布与胜率走势：左轴=局数（胜/负/平堆叠柱状），右轴=胜率（0..100% 简单折线，无端点圆点）。
     */
    static final class FrameOutcomeChart extends ChartPanel {
        private int[] frames = new int[0];
        private int[] statuses = new int[0];
        private FrameBins bins;
        private Rectangle[] binRects;

        void setData(int[] frames, int[] statuses) {
            this.frames = frames;
            this.statuses = statuses;
            this.bins = null;
            this.binRects = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getWidth() < 60 || getHeight() < 48) {
                g2.dispose();
                return;
            }
            if (bins == null) {
                bins = new FrameBins(frames, statuses);
            }
            g2.setFont(I18n.font(Font.PLAIN, 11));
            drawTitle(g2, I18n.t("matchup.chart.rhythm"));

            int marginRight = 36;
            Rectangle r = new Rectangle(42, 22,
                    Math.max(10, getWidth() - 42 - marginRight),
                    Math.max(10, getHeight() - 46));
            int yMax = bins.maxSamples();
            String[] countLabels = new String[5];
            for (int i = 0; i <= 4; i++) {
                countLabels[i] = String.valueOf((int) Math.round(yMax * i / 4.0));
            }
            drawYGrid(g2, r, 4, countLabels);

            // 右轴：胜率 0..100%
            FontMetrics fm = g2.getFontMetrics();
            for (int i = 0; i <= 4; i++) {
                int y = r.y + r.height - (int) Math.round(r.height * i / 4.0);
                String label = (i * 25) + "%";
                g2.setColor(textColor());
                g2.drawString(label, r.x + r.width + 4, y + fm.getAscent() / 2 - 1);
            }

            double slot = r.width / (double) bins.binCount;
            int barW = Math.max(3, (int) Math.round(slot * 0.7));
            binRects = new Rectangle[bins.binCount];
            for (int b = 0; b < bins.binCount; b++) {
                int x = r.x + (int) Math.round(slot * b + (slot - barW) / 2);
                binRects[b] = new Rectangle(x, r.y, barW, r.height);
                int base = r.y + r.height;
                int hWin = (int) Math.round(r.height * bins.win[b] / (double) yMax);
                int hLose = (int) Math.round(r.height * bins.lose[b] / (double) yMax);
                int hDraw = (int) Math.round(r.height * bins.draw[b] / (double) yMax);
                if (hWin > 0) {
                    g2.setColor(winColor());
                    g2.fillRect(x, base - hWin, barW, hWin);
                    base -= hWin;
                }
                if (hLose > 0) {
                    g2.setColor(loseColor());
                    g2.fillRect(x, base - hLose, barW, hLose);
                    base -= hLose;
                }
                if (hDraw > 0) {
                    g2.setColor(neutralColor());
                    g2.fillRect(x, base - hDraw, barW, hDraw);
                }
            }

            // 胜率折线：无端点圆点，跳过空箱（相邻非空箱之间连线）
            g2.setColor(lineColor());
            g2.setStroke(new BasicStroke(1.8f));
            int prevX = -1;
            int prevY = -1;
            for (int b = 0; b < bins.binCount; b++) {
                if (bins.samples(b) == 0) {
                    continue;
                }
                int cx = r.x + (int) Math.round(slot * b + slot / 2);
                int cy = r.y + r.height - (int) Math.round(r.height * bins.winRate(b) / 100.0);
                if (prevX >= 0) {
                    g2.drawLine(prevX, prevY, cx, cy);
                }
                prevX = cx;
                prevY = cy;
            }

            int step = Math.max(1, bins.binCount / 6);
            drawXLabels(g2, r, bins.binCount, step, b -> formatFrames(b * bins.binWidth));
            drawLegend(g2, getWidth(), winLoseDrawLegend(), winLoseDrawColors(),
                    I18n.t("matchup.heatmap.rate"));
            g2.dispose();
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            if (binRects == null) {
                return null;
            }
            for (int b = 0; b < binRects.length; b++) {
                if (binRects[b] != null && binRects[b].contains(event.getPoint())) {
                    String range = I18n.t("matchup.tip.range", b * bins.binWidth, (b + 1) * bins.binWidth - 1);
                    String stats = I18n.t("matchup.tip.stats", bins.win[b], bins.lose[b], bins.draw[b],
                            rateText(bins.winRate(b)));
                    return tipHtml(range, stats);
                }
            }
            return null;
        }
    }

    /** 赢面分布：获胜方剩余 HP（10 格），我方胜为绿、我方负为红。 */
    static final class WinnerHpChart extends ChartPanel {
        private int[] winnerHp = new int[0];
        private int[] statuses = new int[0];
        private final int[] win = new int[10];
        private final int[] lose = new int[10];
        private boolean built;
        private Rectangle[] binRects;

        void setData(int[] winnerHp, int[] statuses) {
            this.winnerHp = winnerHp;
            this.statuses = statuses;
            this.built = false;
            this.binRects = null;
            repaint();
        }

        private void build() {
            java.util.Arrays.fill(win, 0);
            java.util.Arrays.fill(lose, 0);
            int n = Math.min(winnerHp.length, statuses.length);
            for (int i = 0; i < n; i++) {
                int hp = Math.max(0, Math.min(100, winnerHp[i]));
                int b = Math.min(9, hp / 10);
                if (statuses[i] == 1) {
                    win[b]++;
                } else if (statuses[i] == 2) {
                    lose[b]++;
                }
            }
            built = true;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getWidth() < 60 || getHeight() < 48) {
                g2.dispose();
                return;
            }
            if (!built) {
                build();
            }
            g2.setFont(I18n.font(Font.PLAIN, 11));
            drawTitle(g2, I18n.t("matchup.chart.winnerHp"));

            Rectangle r = new Rectangle(42, 22,
                    Math.max(10, getWidth() - 52),
                    Math.max(10, getHeight() - 46));
            int yMax = 1;
            for (int b = 0; b < 10; b++) {
                yMax = Math.max(yMax, win[b] + lose[b]);
            }
            String[] labels = new String[5];
            for (int i = 0; i <= 4; i++) {
                labels[i] = String.valueOf((int) Math.round(yMax * i / 4.0));
            }
            drawYGrid(g2, r, 4, labels);

            double slot = r.width / 10.0;
            int barW = Math.max(4, (int) Math.round(slot * 0.7));
            binRects = new Rectangle[10];
            for (int b = 0; b < 10; b++) {
                int x = r.x + (int) Math.round(slot * b + (slot - barW) / 2);
                binRects[b] = new Rectangle(x, r.y, barW, r.height);
                int base = r.y + r.height;
                int hWin = (int) Math.round(r.height * win[b] / (double) yMax);
                int hLose = (int) Math.round(r.height * lose[b] / (double) yMax);
                if (hWin > 0) {
                    g2.setColor(winColor());
                    g2.fillRect(x, base - hWin, barW, hWin);
                    base -= hWin;
                }
                if (hLose > 0) {
                    g2.setColor(loseColor());
                    g2.fillRect(x, base - hLose, barW, hLose);
                }
            }
            drawXLabels(g2, r, 10, 2, b -> String.valueOf(b * 10));
            drawLegend(g2, getWidth(), winLoseDrawLegend(), winLoseDrawColors());
            g2.dispose();
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            if (binRects == null) {
                return null;
            }
            for (int b = 0; b < binRects.length; b++) {
                if (binRects[b] != null && binRects[b].contains(event.getPoint())) {
                    String range = I18n.t("matchup.tip.range", b * 10, b * 10 + 9);
                    String stats = I18n.t("matchup.tip.statsSimple", win[b], lose[b]);
                    return tipHtml(range, stats);
                }
            }
            return null;
        }
    }

    /**
     * 加速度相性：按对手阵型的加速度等级（红加速器 +1、蓝加速器 +2）统计我方胜率。
     * 柱高 = 胜率（0..100%，平局计半胜），颜色红→黄→绿；柱顶标注胜率，悬停显示明细。
     */
    static final class AccelRateChart extends ChartPanel {
        private List<MatchupAnalyzer.AccelRow> rows = List.of();
        private Rectangle[] barRects;

        void setData(List<MatchupAnalyzer.AccelRow> rows) {
            this.rows = rows == null ? List.of() : rows;
            this.barRects = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getWidth() < 60 || getHeight() < 48) {
                g2.dispose();
                return;
            }
            g2.setFont(I18n.font(Font.PLAIN, 11));
            drawTitle(g2, I18n.t("matchup.tab.accel"));

            Rectangle r = new Rectangle(42, 30,
                    Math.max(10, getWidth() - 52),
                    Math.max(10, getHeight() - 72));
            drawYGrid(g2, r, 4, new String[]{"0%", "25%", "50%", "75%", "100%"});

            // 50% 基准线
            g2.setColor(textColor());
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{4f, 4f}, 0f));
            int y50 = r.y + r.height - (int) Math.round(r.height * 0.5);
            g2.drawLine(r.x, y50, r.x + r.width, y50);
            g2.setStroke(new BasicStroke());

            if (rows.isEmpty()) {
                g2.dispose();
                return;
            }
            double slot = r.width / (double) rows.size();
            int barW = Math.max(4, Math.min(44, (int) Math.round(slot * 0.55)));
            FontMetrics fm = g2.getFontMetrics();
            barRects = new Rectangle[rows.size()];
            for (int i = 0; i < rows.size(); i++) {
                MatchupAnalyzer.AccelRow row = rows.get(i);
                int x = r.x + (int) Math.round(slot * i + (slot - barW) / 2.0);
                barRects[i] = new Rectangle(x, r.y, barW, r.height);
                if (!Double.isNaN(row.winRate())) {
                    int h = (int) Math.round(r.height * row.winRate() / 100.0);
                    g2.setColor(rateColorOpaque(row.winRate()));
                    g2.fillRect(x, r.y + r.height - h, barW, h);
                    String rateLabel = String.format("%.1f%%", row.winRate());
                    g2.setColor(textColor());
                    g2.drawString(rateLabel, x + barW / 2 - fm.stringWidth(rateLabel) / 2,
                            r.y + r.height - h - 4);
                }
            }
            drawXLabels(g2, r, rows.size(), 1, i -> I18n.t("matchup.accel.level", rows.get(i).level()));
            // 每级下方标注 胜/负/平 局数，便于识别小样本导致的极端胜率
            g2.setFont(I18n.font(Font.PLAIN, 10));
            FontMetrics smallFm = g2.getFontMetrics();
            Color base = textColor();
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 160));
            for (int i = 0; i < rows.size(); i++) {
                MatchupAnalyzer.AccelRow row = rows.get(i);
                String counts = row.wins() + "/" + row.loses() + "/" + row.draws();
                int x = r.x + (int) Math.round(slot * i + slot / 2.0) - smallFm.stringWidth(counts) / 2;
                g2.drawString(counts, x, r.y + r.height + fm.getAscent() + 2 + smallFm.getHeight());
            }
            g2.dispose();
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            if (barRects == null) {
                return null;
            }
            for (int i = 0; i < barRects.length; i++) {
                if (barRects[i] != null && barRects[i].contains(event.getPoint())) {
                    MatchupAnalyzer.AccelRow row = rows.get(i);
                    String head = I18n.t("matchup.accel.tip.head", row.level());
                    String stats = I18n.t("matchup.accel.tip.stats", row.samples(),
                            row.wins(), row.loses(), row.draws(), rateText(row.winRate()));
                    return tipHtml(head, stats);
                }
            }
            return null;
        }
    }

    /**
     * Keener 修正：横轴为对手的修正胜率（按 Keener 权重加权平均的每局得分率，0.5 = 阵集平均），
     * 纵轴为我方对位结果（胜/平/负 = 100/50/0）。虚线标出我方修正胜率，菱形标出我方实际胜率；
     * 悬停优先显示离鼠标最近的圆点。
     */
    static final class StrengthChart extends ChartPanel {
        private MatchupAnalyzer.StrengthReport report;
        private final List<Rectangle> pointRects = new ArrayList<>();
        private final List<Integer> pointEntries = new ArrayList<>();
        private Rectangle targetRect;

        void setData(MatchupAnalyzer.StrengthReport report) {
            this.report = report;
            pointRects.clear();
            pointEntries.clear();
            targetRect = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (getWidth() < 60 || getHeight() < 48) {
                g2.dispose();
                return;
            }
            g2.setFont(I18n.font(Font.PLAIN, 11));
            MatchupAnalyzer.StrengthReport data = report;
            String title = data == null
                    ? I18n.t("matchup.tab.strength")
                    : I18n.t("matchup.strength.chartTitle", rateText(data.correctedRate()));
            drawTitle(g2, title);

            Rectangle r = new Rectangle(46, 34,
                    Math.max(10, getWidth() - 46 - 18),
                    Math.max(10, getHeight() - 34 - 40));
            drawYGrid(g2, r, 2, new String[]{
                    I18n.t("matchup.strength.yaxis.lose"),
                    I18n.t("matchup.strength.yaxis.draw"),
                    I18n.t("matchup.strength.yaxis.win")});

            if (data == null || data.entries().isEmpty() || Double.isNaN(data.correctedRate())) {
                g2.dispose();
                return;
            }

            double targetRate = Math.max(0.1, Math.min(99.9, data.correctedRate()));
            double lo = targetRate;
            double hi = targetRate;
            for (MatchupAnalyzer.StrengthEntry entry : data.entries()) {
                lo = Math.min(lo, entry.rate());
                hi = Math.max(hi, entry.rate());
            }
            double pad = Math.max(3.0, (hi - lo) * 0.08);
            double xLo = Math.max(0.0, lo - pad);
            double xHi = Math.min(100.0, hi + pad);
            if (xHi - xLo < 10.0) {
                double mid = (xLo + xHi) / 2;
                xLo = Math.max(0.0, mid - 5);
                xHi = Math.min(100.0, mid + 5);
            }
            final double loX = xLo;
            final double hiX = xHi;
            java.util.function.DoubleUnaryOperator xOf =
                    rate -> r.x + (int) Math.round(r.width * (rate - loX) / (hiX - loX));
            java.util.function.DoubleUnaryOperator yOf =
                    pct -> r.y + r.height - (int) Math.round(r.height * pct / 100.0);

            // 对位结果散点
            int radius = 4;
            for (int i = 0; i < data.entries().size(); i++) {
                MatchupAnalyzer.StrengthEntry entry = data.entries().get(i);
                if (entry.status() != 0 && entry.status() != 1 && entry.status() != 2) {
                    continue;
                }
                double pct = entry.status() == 1 ? 100.0 : (entry.status() == 2 ? 0.0 : 50.0);
                int cx = (int) xOf.applyAsDouble(entry.rate());
                int cy = (int) yOf.applyAsDouble(pct);
                g2.setColor(entry.status() == 1 ? winColor()
                        : entry.status() == 2 ? loseColor() : neutralColor());
                g2.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
                pointRects.add(new Rectangle(cx - radius - 2, cy - radius - 2, radius * 2 + 4, radius * 2 + 4));
                pointEntries.add(i);
            }

            // 我方修正胜率虚线 + 实际胜率菱形
            int targetX = (int) xOf.applyAsDouble(targetRate);
            g2.setColor(lineColor());
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{3f, 3f}, 0f));
            g2.drawLine(targetX, r.y, targetX, r.y + r.height);
            g2.setStroke(new BasicStroke());
            if (!Double.isNaN(data.actualRate())) {
                int cy = (int) yOf.applyAsDouble(data.actualRate());
                int rad = 5;
                int[] xs = {targetX, targetX + rad, targetX, targetX - rad};
                int[] ys = {cy - rad, cy, cy + rad, cy};
                g2.fillPolygon(xs, ys, 4);
                g2.setColor(textColor());
                g2.drawPolygon(xs, ys, 4);
                targetRect = new Rectangle(targetX - rad - 2, cy - rad - 2, rad * 2 + 4, rad * 2 + 4);
            }
            g2.setFont(I18n.font(Font.PLAIN, 10));
            FontMetrics smallFm = g2.getFontMetrics();
            String youLabel = I18n.t("matchup.strength.you");
            int labelX = targetX + 4;
            if (labelX + smallFm.stringWidth(youLabel) > r.x + r.width) {
                labelX = targetX - 4 - smallFm.stringWidth(youLabel);
            }
            g2.setColor(textColor());
            g2.drawString(youLabel, labelX, r.y + smallFm.getAscent() + 2);

            // 横轴刻度（修正胜率）与轴名
            g2.setFont(I18n.font(Font.PLAIN, 11));
            FontMetrics fm = g2.getFontMetrics();
            g2.setColor(textColor());
            for (int i = 0; i <= 4; i++) {
                double rate = loX + (hiX - loX) * i / 4;
                String label = String.format("%.0f%%", rate);
                int cx = (int) xOf.applyAsDouble(rate);
                g2.drawString(label, cx - fm.stringWidth(label) / 2, r.y + r.height + fm.getAscent() + 2);
            }
            String axis = I18n.t("matchup.strength.axis");
            g2.drawString(axis, r.x + r.width / 2 - fm.stringWidth(axis) / 2,
                    r.y + r.height + fm.getAscent() + 2 + fm.getHeight());
            drawLegend(g2, getWidth(), winLoseDrawLegend(), winLoseDrawColors());
            g2.dispose();
        }

        /** 悬停取离鼠标最近的命中圆点（我方菱形或对位圆点）。 */
        @Override
        public String getToolTipText(MouseEvent event) {
            if (report == null) {
                return null;
            }
            int ex = event.getX();
            int ey = event.getY();
            Rectangle best = null;
            boolean targetHit = false;
            int bestIndex = -1;
            double bestDist = Double.MAX_VALUE;
            if (targetRect != null && targetRect.contains(ex, ey)) {
                best = targetRect;
                targetHit = true;
                bestDist = dist2(targetRect, ex, ey);
            }
            for (int i = 0; i < pointRects.size(); i++) {
                Rectangle rect = pointRects.get(i);
                if (!rect.contains(ex, ey)) {
                    continue;
                }
                double d = dist2(rect, ex, ey);
                if (d < bestDist) {
                    best = rect;
                    targetHit = false;
                    bestIndex = i;
                    bestDist = d;
                }
            }
            if (best == null) {
                return null;
            }
            if (targetHit) {
                String head = I18n.t("matchup.strength.tip.you", rateText(report.correctedRate()));
                double delta = report.delta();
                String deltaText = Double.isNaN(delta) ? "—" : String.format("%+.1f%%", delta);
                String stats = I18n.t("matchup.strength.tip.youStats",
                        report.targetRank(), report.poolSize() + 1,
                        rateText(report.actualRate()), deltaText);
                return tipHtml(head, stats);
            }
            MatchupAnalyzer.StrengthEntry entry = report.entries().get(pointEntries.get(bestIndex));
            String head = I18n.t("matchup.strength.tip.head", entry.name(), rateText(entry.rate()));
            String result = entry.status() == 1 ? I18n.t("matchup.legend.win")
                    : entry.status() == 2 ? I18n.t("matchup.legend.lose")
                    : I18n.t("matchup.legend.draw");
            String stats = I18n.t("matchup.strength.tip.stats",
                    entry.rank(), report.poolSize() + 1, result);
            return tipHtml(head, stats);
        }

        private static double dist2(Rectangle rect, int x, int y) {
            double dx = x - rect.getCenterX();
            double dy = y - rect.getCenterY();
            return dx * dx + dy * dy;
        }
    }
}
