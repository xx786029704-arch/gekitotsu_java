package org.example.GUI;

import org.example.BattlefieldAnalyzer;
import org.example.I18n;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

/**
 * 战场空间的两个图表：
 * <ul>
 *   <li>{@link TimelineChart} — 推进曲线：我方/对方 HP、存活、弹幕随时间（按胜/负分组平均）；</li>
 *   <li>{@link DistributionChart} — 分布直方图（开局压迫：首次接触帧、双方核心首次受击帧）。</li>
 * </ul>
 * 全部自绘，配色随深色模式。
 */
final class BattlefieldCharts {

    private BattlefieldCharts() {
    }

    /** 推进曲线指标。 */
    enum TimelineMetric {
        HP1, HP2, ALIVE1, ALIVE2, ATK1, ATK2
    }

    private static Color textColor() {
        return MatchupCharts.textColor();
    }

    private static Color gridColor() {
        return MatchupCharts.gridColor();
    }

    private static void drawTitle(Graphics2D g2, String title) {
        Font old = g2.getFont();
        g2.setFont(I18n.font(Font.BOLD, 12));
        g2.setColor(textColor());
        g2.drawString(title, 6, 14);
        g2.setFont(old);
    }

    private static void drawYGrid(Graphics2D g2, Rectangle r, int divisions, String[] labels) {
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i <= divisions; i++) {
            int y = r.y + r.height - (int) Math.round(r.height * i / (double) divisions);
            g2.setColor(gridColor());
            g2.drawLine(r.x, y, r.x + r.width, y);
            g2.setColor(textColor());
            String label = labels[i];
            g2.drawString(label, r.x - 4 - fm.stringWidth(label), y + fm.getAscent() / 2 - 1);
        }
    }

    /** 推进曲线系列数：0=平均，1=我方胜，2=我方负，3=其他。 */
    static final int SERIES_COUNT = 4;

    static String seriesLabel(int series) {
        return switch (series) {
            case 0 -> I18n.t("battlefield.legend.mean");
            case 1 -> I18n.t("battlefield.legend.win");
            case 2 -> I18n.t("battlefield.legend.lose");
            default -> I18n.t("battlefield.legend.other");
        };
    }

    static Color seriesColor(int series) {
        return switch (series) {
            case 0 -> MatchupCharts.lineColor();
            case 1 -> MatchupCharts.winColor();
            case 2 -> MatchupCharts.loseColor();
            default -> MatchupCharts.neutralColor();
        };
    }

    /** 推进曲线：x=帧，y=指标平均值；系列为平均与胜/负/其他分组，可勾选显示。 */
    static final class TimelineChart extends JPanel {
        private BattlefieldAnalyzer.Report report;
        private TimelineMetric metric = TimelineMetric.HP1;
        private Rectangle[] binRects;
        /** 系列可见性（0=平均，1=胜，2=负，3=其他）；默认开启平均/胜/负。 */
        private final boolean[] visible = {true, true, true, false};

        TimelineChart() {
            setToolTipText("");
            ToolTipManager.sharedInstance().registerComponent(this);
        }

        void setReport(BattlefieldAnalyzer.Report report) {
            this.report = report;
            this.binRects = null;
            repaint();
        }

        void setMetric(TimelineMetric metric) {
            this.metric = metric;
            repaint();
        }

        void setSeriesVisible(int series, boolean value) {
            if (series >= 0 && series < SERIES_COUNT) {
                visible[series] = value;
                repaint();
            }
        }

        private int column() {
            return switch (metric) {
                case HP1 -> 0;
                case HP2 -> 1;
                case ALIVE1 -> 2;
                case ALIVE2 -> 3;
                case ATK1 -> 4;
                case ATK2 -> 5;
            };
        }

        private double average(int group, int bin) {
            double count = report.timelineCount(group, bin);
            return count > 0 ? report.timelineValue(group, bin, column()) / count : Double.NaN;
        }

        /** 系列值：0=全部对局的均值，1..3=胜/负/其他分组均值。 */
        private double seriesValue(int series, int bin) {
            if (series > 0) {
                return average(series - 1, bin);
            }
            double sum = 0;
            double count = 0;
            for (int group = 0; group < BattlefieldAnalyzer.GROUPS; group++) {
                sum += report.timelineValue(group, bin, column());
                count += report.timelineCount(group, bin);
            }
            return count > 0 ? sum / count : Double.NaN;
        }

        private double seriesCount(int series, int bin) {
            if (series > 0) {
                return report.timelineCount(series - 1, bin);
            }
            double count = 0;
            for (int group = 0; group < BattlefieldAnalyzer.GROUPS; group++) {
                count += report.timelineCount(group, bin);
            }
            return count;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (w < 60 || h < 48 || report == null) {
                g2.dispose();
                return;
            }
            g2.setFont(I18n.font(Font.PLAIN, 11));

            Rectangle r = new Rectangle(42, 22, Math.max(10, w - 52), Math.max(10, h - 46));
            int bins = report.bins();
            // 横轴按最大对局时长动态收缩：取最后一个有样本的箱
            int lastBin = -1;
            for (int bin = 0; bin < bins; bin++) {
                for (int group = 0; group < BattlefieldAnalyzer.GROUPS; group++) {
                    if (report.timelineCount(group, bin) > 0) {
                        lastBin = bin;
                        break;
                    }
                }
            }
            if (lastBin < 0) {
                g2.dispose();
                return;
            }
            int usedBins = lastBin + 1;
            double yMax = 1;
            for (int series = 0; series < SERIES_COUNT; series++) {
                if (!visible[series]) {
                    continue;
                }
                for (int bin = 0; bin < usedBins; bin++) {
                    double v = seriesValue(series, bin);
                    if (!Double.isNaN(v)) {
                        yMax = Math.max(yMax, v);
                    }
                }
            }
            yMax = niceCeil(yMax);
            String[] labels = new String[5];
            for (int i = 0; i <= 4; i++) {
                labels[i] = formatValue(yMax * i / 4.0);
            }
            drawYGrid(g2, r, 4, labels);

            double slot = r.width / (double) usedBins;
            binRects = new Rectangle[usedBins];
            for (int bin = 0; bin < usedBins; bin++) {
                binRects[bin] = new Rectangle(r.x + (int) Math.round(slot * bin), r.y,
                        Math.max(2, (int) Math.round(slot)), r.height);
            }
            // 分组曲线先画，均值曲线最后画在最上层
            for (int series = 1; series < SERIES_COUNT; series++) {
                drawSeries(g2, r, slot, usedBins, yMax, series);
            }
            drawSeries(g2, r, slot, usedBins, yMax, 0);

            // x 轴刻度（帧）
            FontMetrics fm = g2.getFontMetrics();
            int step = Math.max(1, usedBins / 6);
            g2.setColor(textColor());
            for (int bin = 0; bin < usedBins; bin += step) {
                String label = formatFrames(bin * report.binFrames());
                int x = r.x + (int) Math.round(slot * bin + slot / 2) - fm.stringWidth(label) / 2;
                g2.drawString(label, x, r.y + r.height + fm.getAscent() + 2);
            }
            g2.dispose();
        }

        private void drawSeries(Graphics2D g2, Rectangle r, double slot, int usedBins,
                                double yMax, int series) {
            if (!visible[series]) {
                return;
            }
            g2.setColor(seriesColor(series));
            g2.setStroke(new BasicStroke(series == 0 ? 2.0f : 1.8f));
            int prevX = -1;
            int prevY = -1;
            for (int bin = 0; bin < usedBins; bin++) {
                double v = seriesValue(series, bin);
                if (Double.isNaN(v)) {
                    continue;
                }
                int cx = r.x + (int) Math.round(slot * bin + slot / 2);
                int cy = r.y + r.height - (int) Math.round(r.height * v / yMax);
                if (prevX >= 0) {
                    g2.drawLine(prevX, prevY, cx, cy);
                }
                prevX = cx;
                prevY = cy;
            }
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            if (report == null || binRects == null) {
                return null;
            }
            for (int bin = 0; bin < binRects.length; bin++) {
                if (binRects[bin] != null && binRects[bin].contains(event.getPoint())) {
                    StringBuilder sb = new StringBuilder("<html>");
                    sb.append(I18n.t("battlefield.timeline.tip",
                            bin * report.binFrames(), (bin + 1) * report.binFrames() - 1));
                    for (int series = 0; series < SERIES_COUNT; series++) {
                        if (!visible[series]) {
                            continue;
                        }
                        double count = seriesCount(series, bin);
                        if (count > 0) {
                            sb.append("<br>").append(seriesLabel(series)).append(": ")
                                    .append(formatValue(seriesValue(series, bin)))
                                    .append(" (").append((int) count).append(")");
                        }
                    }
                    sb.append("</html>");
                    return sb.toString();
                }
            }
            return null;
        }

        private static double niceCeil(double value) {
            double step = Math.pow(10, Math.floor(Math.log10(Math.max(1, value))));
            while (step * 10 < value) {
                step *= 10;
            }
            if (value <= step) {
                step /= 2;
            }
            return Math.ceil(value / step) * step;
        }

        private static String formatValue(double value) {
            if (value >= 1000) {
                return String.format("%.1fk", value / 1000.0);
            }
            return value == Math.rint(value) ? String.valueOf((long) value)
                    : String.format("%.1f", value);
        }

        static String formatFrames(int frames) {
            return frames >= 1000 ? String.format("%.1fk", frames / 1000.0) : String.valueOf(frames);
        }
    }

    /** 分布直方图（最多两条系列并排，用于开局压迫）。 */
    static final class DistributionChart extends JPanel {
        private final String titleKey;
        private double[] valuesA = new double[0];
        private double[] valuesB = new double[0];
        private boolean hasB;
        private Color colorA = new Color(0xFFB74D);
        private Color colorB = MatchupCharts.winColor();
        private String labelAKey = "";
        private String labelBKey = "";
        private int neverA;
        private int neverB;
        private Rectangle[] binRects;
        private int binWidth;

        DistributionChart(String titleKey) {
            this.titleKey = titleKey;
            setToolTipText("");
            ToolTipManager.sharedInstance().registerComponent(this);
        }

        void setData(double[] valuesA, int neverA, Color colorA) {
            this.valuesA = valuesA == null ? new double[0] : valuesA;
            this.neverA = neverA;
            this.colorA = colorA;
            this.valuesB = new double[0];
            this.hasB = false;
            this.neverB = 0;
            this.binRects = null;
            repaint();
        }

        void setData(double[] valuesA, int neverA, Color colorA, String labelAKey,
                     double[] valuesB, int neverB, Color colorB, String labelBKey) {
            this.valuesA = valuesA == null ? new double[0] : valuesA;
            this.neverA = neverA;
            this.colorA = colorA;
            this.labelAKey = labelAKey;
            this.valuesB = valuesB == null ? new double[0] : valuesB;
            this.neverB = neverB;
            this.colorB = colorB;
            this.labelBKey = labelBKey;
            this.hasB = true;
            this.binRects = null;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            if (w < 60 || h < 48) {
                g2.dispose();
                return;
            }
            g2.setFont(I18n.font(Font.PLAIN, 11));
            drawTitle(g2, I18n.t(titleKey));

            Rectangle r = new Rectangle(42, 22, Math.max(10, w - 52), Math.max(10, h - 46));
            int max = 0;
            for (double v : valuesA) {
                max = Math.max(max, (int) v);
            }
            for (double v : valuesB) {
                max = Math.max(max, (int) v);
            }
            max = Math.max(max, 100);
            int bins = 20;
            binWidth = Math.max(1, (max + bins - 1) / bins);
            int[] countA = new int[bins];
            int[] countB = new int[bins];
            for (double v : valuesA) {
                countA[Math.min(bins - 1, (int) v / binWidth)]++;
            }
            for (double v : valuesB) {
                countB[Math.min(bins - 1, (int) v / binWidth)]++;
            }
            int yMax = 1;
            for (int i = 0; i < bins; i++) {
                yMax = Math.max(yMax, countA[i] + countB[i]);
            }
            String[] labels = new String[5];
            for (int i = 0; i <= 4; i++) {
                labels[i] = String.valueOf((int) Math.round(yMax * i / 4.0));
            }
            drawYGrid(g2, r, 4, labels);

            double slot = r.width / (double) bins;
            int pairW = Math.max(4, (int) Math.round(slot * 0.7));
            int barW = hasB ? Math.max(2, pairW / 2 - 1) : pairW;
            binRects = new Rectangle[bins];
            for (int bin = 0; bin < bins; bin++) {
                int x = r.x + (int) Math.round(slot * bin + (slot - pairW) / 2);
                binRects[bin] = new Rectangle(r.x + (int) Math.round(slot * bin), r.y,
                        Math.max(2, (int) Math.round(slot)), r.height);
                int base = r.y + r.height;
                int hA = (int) Math.round(r.height * countA[bin] / (double) yMax);
                if (hA > 0) {
                    g2.setColor(colorA);
                    g2.fillRect(x, base - hA, barW, hA);
                }
                if (hasB) {
                    int hB = (int) Math.round(r.height * countB[bin] / (double) yMax);
                    if (hB > 0) {
                        g2.setColor(colorB);
                        g2.fillRect(x + barW + 2, base - hB, barW, hB);
                    }
                }
            }
            FontMetrics fm = g2.getFontMetrics();
            int step = Math.max(1, bins / 5);
            g2.setColor(textColor());
            for (int bin = 0; bin < bins; bin += step) {
                String label = TimelineChart.formatFrames(bin * binWidth);
                int x = r.x + (int) Math.round(slot * bin + slot / 2) - fm.stringWidth(label) / 2;
                g2.drawString(label, x, r.y + r.height + fm.getAscent() + 2);
            }
            drawSeriesLegend(g2, w);
            g2.dispose();
        }

        private void drawSeriesLegend(Graphics2D g2, int w) {
            FontMetrics fm = g2.getFontMetrics();
            int x = w - 6;
            int y = 11;
            if (hasB && !labelBKey.isEmpty()) {
                String text = I18n.t(labelBKey) + (neverB > 0 ? " (" + I18n.t("battlefield.pressure.never", neverB) + ")" : "");
                x -= fm.stringWidth(text);
                g2.setColor(textColor());
                g2.drawString(text, x, y);
                x -= 5 + 8;
                g2.setColor(colorB);
                g2.fillRect(x, y - 8, 8, 8);
                x -= 10;
            }
            if (!labelAKey.isEmpty()) {
                String text = I18n.t(labelAKey) + (neverA > 0 ? " (" + I18n.t("battlefield.pressure.never", neverA) + ")" : "");
                x -= fm.stringWidth(text);
                g2.setColor(textColor());
                g2.drawString(text, x, y);
                x -= 5 + 8;
                g2.setColor(colorA);
                g2.fillRect(x, y - 8, 8, 8);
            }
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            if (binRects == null) {
                return null;
            }
            for (int bin = 0; bin < binRects.length; bin++) {
                if (binRects[bin] != null && binRects[bin].contains(event.getPoint())) {
                    int countA = 0;
                    int countB = 0;
                    for (double v : valuesA) {
                        if ((int) v / binWidth == bin) {
                            countA++;
                        }
                    }
                    for (double v : valuesB) {
                        if ((int) v / binWidth == bin) {
                            countB++;
                        }
                    }
                    String range = I18n.t("battlefield.tip.range", bin * binWidth, (bin + 1) * binWidth - 1);
                    String stats = hasB
                            ? I18n.t("battlefield.tip.two", countA, countB)
                            : I18n.t("battlefield.tip.one", countA);
                    return "<html>" + range + "<br>" + stats + "</html>";
                }
            }
            return null;
        }
    }
}
