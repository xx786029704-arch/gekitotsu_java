package org.example.GUI;

import org.example.I18n;
import org.example.Main;
import org.example.MatchupAnalyzer;
import org.example.Setting;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 行为分析 → 对阵画像：从结果级数据统计原始阵型面对测试阵集时的特征。
 *
 * <p>左侧为配置列（约占 1/4 宽），右侧为结果列，中间分隔线可拖动；底部为进度栏。
 * 结果含核心位置胜率热力图、单位敏感度排行、节奏与赢面、加速度相性、Keener 修正五页。
 * 仅使用对战结果，不采集遥测。
 */
public class MatchupProfilePanel extends BehaviorAnalysisPanel {

    // —— 配置 ——
    private final JSpinner threadSpinner = new JSpinner(
            new SpinnerNumberModel(Math.min(256, Math.max(1, Main.MAX_THREADS)), 1, 256, 1));
    private final JSpinner smoothingSpinner = new JSpinner(new SpinnerNumberModel(20, 8, 60, 4));
    private final JCheckBox strengthBox = new JCheckBox(I18n.t("matchup.strength.enable"), true);
    private final JTextArea infoArea = new JTextArea();
    private final JButton startButton = new JButton(I18n.t("matchup.start"));
    private final JButton stopButton = new JButton(I18n.t("matchup.stop"));

    // —— 进度 ——
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel statusLabel = new JLabel(I18n.t("matchup.ready"));
    private final JLabel timeLabel = new JLabel(" ");

    // —— 结果 ——
    private final JLabel summaryLabel = new JLabel(" ");
    private final JTabbedPane resultTabs = new JTabbedPane();
    private final CoreHeatmapPanel heatmap = new CoreHeatmapPanel();
    private final MatchupCharts.FrameOutcomeChart outcomeChart = new MatchupCharts.FrameOutcomeChart();
    private final MatchupCharts.WinnerHpChart winnerHpChart = new MatchupCharts.WinnerHpChart();
    private final MatchupCharts.AccelRateChart accelChart = new MatchupCharts.AccelRateChart();
    private final MatchupCharts.StrengthChart strengthChart = new MatchupCharts.StrengthChart();
    private final SensitivityTableModel sensitivityModel = new SensitivityTableModel();
    private final JTable sensitivityTable = new JTable(sensitivityModel);
    private final TableRowSorter<SensitivityTableModel> sensitivitySorter = new TableRowSorter<>(sensitivityModel);

    // —— 汇总统计 ——
    private final JLabel avgFramesValue = new JLabel("—");
    private final JLabel avgWinFramesValue = new JLabel("—");
    private final JLabel avgLoseFramesValue = new JLabel("—");
    private final JLabel avgWinHpValue = new JLabel("—");
    private final JLabel avgLoseHpValue = new JLabel("—");

    // —— Keener 修正 ——
    private final JLabel strengthRankCaption = new JLabel(I18n.t("matchup.strength.rank"));
    private final JLabel strengthActualCaption = new JLabel(I18n.t("matchup.strength.actual"));
    private final JLabel strengthCorrectedCaption = new JLabel(I18n.t("matchup.strength.corrected"));
    private final JLabel strengthRankValue = new JLabel("—");
    private final JLabel strengthActualValue = new JLabel("—");
    private final JLabel strengthCorrectedValue = new JLabel("—");
    private final JLabel strengthHintLabel = new JLabel(" ");

    // —— 状态 ——
    private boolean analyzing = false;
    private int evalCount = 0;
    private boolean playerReady = false;
    private boolean strengthRequested = false;
    private SwingWorker<MatchupAnalyzer.Report, Void> worker;
    private long startTime;

    public MatchupProfilePanel(Supplier<BehaviorInput> inputs, Consumer<Boolean> busyListener) {
        super(inputs, busyListener);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        smoothingSpinner.setToolTipText(I18n.t("matchup.smoothingTip"));
        threadSpinner.setToolTipText(I18n.t("matchup.threadsTip"));
        strengthBox.setToolTipText(I18n.t("matchup.strength.enableTip"));

        add(buildSplit(), BorderLayout.CENTER);
        add(buildProgressPanel(), BorderLayout.SOUTH);

        wireEvents();
        installRenderers();
        updateStartState();
    }

    @Override
    public String titleKey() {
        return "behavior.item.matchup";
    }

    @Override
    public void onInputChanged(BehaviorInput input) {
        refreshInputState(input);
    }

    @Override
    public void updateDarkMode() {
        repaint();
        accelChart.repaint();
        strengthChart.repaint();
        heatmap.repaint();
        outcomeChart.repaint();
        winnerHpChart.repaint();
        sensitivityTable.repaint();
    }

    /** 左右分栏：配置 1/4、结果 3/4，窗口缩放时保持比例，分隔线可拖动。 */
    private JSplitPane buildSplit() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildConfigPanel(), buildResultPanel());
        split.setResizeWeight(0.25);
        split.setContinuousLayout(true);
        split.setDividerSize(6);
        split.setBorder(null);
        SplitRatio.applyOnFirstShow(split, 0.25);
        return split;
    }

    private JPanel buildConfigPanel() {
        JPanel rows = new JPanel();
        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.add(paramRow(new JLabel(I18n.t("matchup.threads")), threadSpinner));
        rows.add(paramRow(new JLabel(I18n.t("matchup.smoothing")), smoothingSpinner));
        rows.add(paramRow(strengthBox));
        rows.add(paramRow(startButton, stopButton));
        stopButton.setEnabled(false);

        infoArea.setEditable(false);
        infoArea.setFocusable(false);
        infoArea.setOpaque(false);
        infoArea.setLineWrap(true);
        infoArea.setWrapStyleWord(true);
        infoArea.setFont(I18n.font(Font.PLAIN, 12));

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder(I18n.t("matchup.config")));
        panel.setMinimumSize(new Dimension(180, 0));
        panel.add(rows, BorderLayout.NORTH);
        panel.add(infoArea, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel paramRow(JComponent... components) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (JComponent component : components) {
            row.add(component);
        }
        return row;
    }

    private JPanel buildResultPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setBorder(BorderFactory.createTitledBorder(I18n.t("matchup.result")));
        summaryLabel.setFont(I18n.font(Font.PLAIN, 13));
        panel.add(summaryLabel, BorderLayout.NORTH);

        JPanel coreTab = new JPanel(new BorderLayout(0, 4));
        JLabel coreIntro = new JLabel(I18n.t("matchup.core.intro"));
        coreIntro.setFont(I18n.font(Font.PLAIN, 12));
        coreTab.add(coreIntro, BorderLayout.NORTH);
        coreTab.add(heatmap, BorderLayout.CENTER);
        resultTabs.addTab(I18n.t("matchup.tab.core"), coreTab);
        resultTabs.addTab(I18n.t("matchup.tab.sensitivity"), new JScrollPane(sensitivityTable));
        resultTabs.addTab(I18n.t("matchup.tab.rhythm"), buildRhythmPanel());
        resultTabs.addTab(I18n.t("matchup.tab.accel"), buildAccelPanel());
        resultTabs.addTab(I18n.t("matchup.tab.strength"), buildStrengthPanel());
        panel.add(resultTabs, BorderLayout.CENTER);
        return panel;
    }

    /** Keener 修正：说明与指导意义 + 三项指标（阵集排名/实际胜率/修正胜率）+ 散点图。 */
    private JPanel buildStrengthPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        JPanel head = new JPanel();
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        JLabel intro = new JLabel(I18n.t("matchup.strength.intro"));
        intro.setFont(I18n.font(Font.PLAIN, 12));
        intro.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel intro2 = new JLabel(I18n.t("matchup.strength.intro2"));
        intro2.setFont(I18n.font(Font.PLAIN, 12));
        intro2.setAlignmentX(Component.LEFT_ALIGNMENT);
        strengthHintLabel.setFont(I18n.font(Font.PLAIN, 12));
        strengthHintLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel kpis = new JPanel(new GridLayout(1, 3, 12, 0));
        kpis.setAlignmentX(Component.LEFT_ALIGNMENT);
        kpis.add(kpiCell(strengthRankCaption, strengthRankValue));
        kpis.add(kpiCell(strengthActualCaption, strengthActualValue));
        kpis.add(kpiCell(strengthCorrectedCaption, strengthCorrectedValue));
        head.add(intro);
        head.add(intro2);
        head.add(strengthHintLabel);
        head.add(Box.createVerticalStrut(4));
        head.add(kpis);
        panel.add(head, BorderLayout.NORTH);
        panel.add(strengthChart, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel kpiCell(JLabel caption, JLabel value) {
        caption.setFont(I18n.font(Font.PLAIN, 11));
        value.setFont(I18n.font(Font.BOLD, 15));
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        value.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel cell = new JPanel();
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.add(caption);
        cell.add(value);
        return cell;
    }

    /** 刷新 Keener 修正指标；未启用或阵集过小时给出提示。 */
    private void updateStrengthKpis(MatchupAnalyzer.Report report) {
        if (!strengthRequested) {
            strengthHintLabel.setText(I18n.t("matchup.strength.off"));
        } else if (evalCount < MatchupAnalyzer.STRENGTH_MIN_POOL) {
            strengthHintLabel.setText(I18n.t("matchup.strength.tooSmall", MatchupAnalyzer.STRENGTH_MIN_POOL));
        } else {
            strengthHintLabel.setText(" ");
        }
        MatchupAnalyzer.StrengthReport s = report.strength();
        if (s == null) {
            resetStrengthKpis();
            return;
        }
        strengthRankCaption.setText(I18n.t("matchup.strength.rank"));
        strengthActualCaption.setText(I18n.t("matchup.strength.actual") + gamesSuffix(s.games()));
        strengthCorrectedCaption.setText(I18n.t("matchup.strength.corrected"));
        strengthRankValue.setText(I18n.t("matchup.strength.rankValue", s.targetRank(), s.poolSize() + 1));
        strengthActualValue.setText(MatchupCharts.rateText(s.actualRate()));
        setCorrectedValue(s);
    }

    /** 修正胜率后附变化量（修正 − 实际）：增加用绿字、降低用红字。 */
    private void setCorrectedValue(MatchupAnalyzer.StrengthReport s) {
        String rate = MatchupCharts.rateText(s.correctedRate());
        double delta = s.delta();
        if (Double.isNaN(delta)) {
            strengthCorrectedValue.setText(rate);
            return;
        }
        Color color = delta >= 0 ? upColor() : downColor();
        strengthCorrectedValue.setText(String.format(
                "<html>%s <font color='#%06X'>%+.1f%%</font></html>",
                rate, color.getRGB() & 0xFFFFFF, delta));
    }

    private static String gamesSuffix(int games) {
        return games > 0 ? I18n.t("matchup.strength.games", games) : "";
    }

    private void resetStrengthKpis() {
        strengthRankCaption.setText(I18n.t("matchup.strength.rank"));
        strengthActualCaption.setText(I18n.t("matchup.strength.actual"));
        strengthCorrectedCaption.setText(I18n.t("matchup.strength.corrected"));
        strengthRankValue.setText("—");
        strengthActualValue.setText("—");
        strengthCorrectedValue.setText("—");
    }

    /** 加速度相性：上方说明 + 按对手加速度等级的胜率柱状图。 */
    private JPanel buildAccelPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        JLabel intro = new JLabel(I18n.t("matchup.accel.intro"));
        intro.setFont(I18n.font(Font.PLAIN, 12));
        panel.add(intro, BorderLayout.NORTH);
        panel.add(accelChart, BorderLayout.CENTER);
        return panel;
    }

    /** 左侧上下两幅图表（时长分布与胜率走势、赢面分布），右侧整栏为汇总。 */
    private JPanel buildRhythmPanel() {
        JPanel charts = new JPanel(new GridLayout(2, 1, 6, 6));
        charts.add(outcomeChart);
        charts.add(winnerHpChart);
        JPanel stats = buildStatsPanel();
        stats.setPreferredSize(new Dimension(240, 0));
        JPanel rhythm = new JPanel(new BorderLayout(6, 6));
        rhythm.add(charts, BorderLayout.CENTER);
        rhythm.add(stats, BorderLayout.EAST);
        return rhythm;
    }

    private JPanel buildStatsPanel() {
        JPanel rows = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 8, 2, 8);
        c.anchor = GridBagConstraints.WEST;
        addStatRow(rows, c, 0, I18n.t("matchup.stat.avgFrames"), avgFramesValue);
        addStatRow(rows, c, 1, I18n.t("matchup.stat.avgWinFrames"), avgWinFramesValue);
        addStatRow(rows, c, 2, I18n.t("matchup.stat.avgLoseFrames"), avgLoseFramesValue);
        addStatRow(rows, c, 3, I18n.t("matchup.stat.avgWinHp"), avgWinHpValue);
        addStatRow(rows, c, 4, I18n.t("matchup.stat.avgLoseHp"), avgLoseHpValue);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(I18n.t("matchup.stats")));
        panel.add(rows, BorderLayout.NORTH);
        return panel;
    }

    private static void addStatRow(JPanel panel, GridBagConstraints c, int row, String label, JLabel value) {
        c.gridx = 0;
        c.gridy = row;
        c.weightx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        value.setFont(I18n.font(Font.BOLD, 12));
        panel.add(value, c);
    }

    private JPanel buildProgressPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        progressBar.setStringPainted(true);
        panel.add(progressBar, BorderLayout.NORTH);
        JPanel row = new JPanel(new BorderLayout());
        row.add(statusLabel, BorderLayout.WEST);
        row.add(timeLabel, BorderLayout.EAST);
        panel.add(row, BorderLayout.SOUTH);
        return panel;
    }

    private void wireEvents() {
        startButton.addActionListener(e -> startAnalysis());
        stopButton.addActionListener(e -> stopAnalysis());
        smoothingSpinner.addChangeListener(e -> heatmap.setBandwidth((int) smoothingSpinner.getValue()));
        strengthBox.addActionListener(e -> {
            if (!analyzing) {
                updateStartState();
            }
        });
    }

    /** 输入变化时更新就绪状态与对战规模。 */
    private void refreshInputState(BehaviorInput input) {
        if (analyzing) {
            return;
        }
        evalCount = countForts(input.evalText());
        playerReady = countForts(input.playerText()) == 1;
        updateStartState();
    }

    private void updateStartState() {
        boolean ok = playerReady && evalCount > 0;
        startButton.setEnabled(!analyzing && ok);
        if (!ok) {
            infoArea.setText("");
            return;
        }
        int strengthBattles = strengthBox.isSelected() ? MatchupAnalyzer.strengthBattleEstimate(evalCount) : 0;
        String text = I18n.t("matchup.scaleInfo", evalCount, evalCount + strengthBattles);
        if (strengthBattles > 0) {
            text += "\n" + I18n.t("matchup.strength.scale", strengthBattles);
        }
        infoArea.setText(text);
    }

    private static int countForts(String text) {
        if (text.isEmpty()) {
            return 0;
        }
        try {
            return Setting.parseFortsRaw(text).size();
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    private void startAnalysis() {
        if (analyzing) {
            return;
        }
        BehaviorInput input = currentInput();
        refreshInputState(input);
        if (!playerReady || evalCount == 0) {
            JOptionPane.showMessageDialog(this, I18n.t("matchup.needInput"),
                    I18n.t("msg.tip"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        String playerText = input.playerText();
        String evalText = input.evalText();
        int threads = (int) threadSpinner.getValue();
        final boolean strengthEnabled = strengthBox.isSelected();
        strengthRequested = strengthEnabled;

        analyzing = true;
        setControlsEnabled(false);
        summaryLabel.setText(" ");
        heatmap.setData(List.of(), new int[0]);
        sensitivityModel.setRows(List.of());
        outcomeChart.setData(new int[0], new int[0]);
        winnerHpChart.setData(new int[0], new int[0]);
        accelChart.setData(List.of());
        strengthChart.setData(null);
        strengthHintLabel.setText(" ");
        resetStrengthKpis();
        resetStats();
        progressBar.setMaximum(Math.max(1, evalCount));
        progressBar.setValue(0);
        progressBar.setString("0 / " + evalCount);
        statusLabel.setText(I18n.t("matchup.analyzing"));
        timeLabel.setText(" ");
        startTime = System.nanoTime();

        worker = new SwingWorker<>() {
            @Override
            protected MatchupAnalyzer.Report doInBackground() throws Exception {
                return MatchupAnalyzer.analyze(playerText, evalText, threads, strengthEnabled,
                        new MatchupAnalyzer.ProgressListener() {
                            @Override
                            public void onProgress(int done, int total) {
                                SwingUtilities.invokeLater(() -> {
                                    if (!analyzing) {
                                        return;
                                    }
                                    progressBar.setMaximum(Math.max(1, total));
                                    progressBar.setValue(done);
                                    progressBar.setString(done + " / " + total);
                                    long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
                                    timeLabel.setText(I18n.t("matchup.elapsed", elapsedMs / 1000.0));
                                });
                            }

                            @Override
                            public void onStage(String labelKey) {
                                SwingUtilities.invokeLater(() -> {
                                    if (analyzing) {
                                        statusLabel.setText(I18n.t(labelKey));
                                    }
                                });
                            }
                        }, this::isCancelled);
            }

            @Override
            protected void done() {
                analyzing = false;
                setControlsEnabled(true);
                try {
                    if (isCancelled()) {
                        statusLabel.setText(I18n.t("matchup.cancelled"));
                        progressBar.setString(I18n.t("matchup.cancelled"));
                        return;
                    }
                    MatchupAnalyzer.Report report = get();
                    if (report == null) {
                        statusLabel.setText(I18n.t("matchup.cancelled"));
                        progressBar.setString(I18n.t("matchup.cancelled"));
                        return;
                    }
                    showReport(report);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    statusLabel.setText(I18n.t("matchup.fail") + cause.getMessage());
                    JOptionPane.showMessageDialog(MatchupProfilePanel.this,
                            I18n.t("matchup.fail") + "\n" + cause.getMessage(),
                            I18n.t("msg.error"), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void stopAnalysis() {
        if (worker != null) {
            worker.cancel(true);
        }
    }

    private void setControlsEnabled(boolean enabled) {
        threadSpinner.setEnabled(enabled);
        smoothingSpinner.setEnabled(enabled);
        strengthBox.setEnabled(enabled);
        stopButton.setEnabled(!enabled);
        setInputBusy(!enabled);
        if (enabled) {
            updateStartState();
        } else {
            startButton.setEnabled(false);
        }
    }

    private void showReport(MatchupAnalyzer.Report report) {
        String summary = I18n.t("matchup.summary", report.total(), report.win(), report.lose(),
                report.draw(), report.timeout(), MatchupCharts.rateText(report.winRate()),
                report.elapsedMs() / 1000.0);
        if (report.battleErrors() > 0) {
            summary += I18n.t("matchup.errors", report.battleErrors());
        }
        summaryLabel.setText(summary);
        heatmap.setData(report.evalForts(), report.statuses());
        sensitivityModel.setRows(report.sensitivity());
        outcomeChart.setData(report.frames(), report.statuses());
        winnerHpChart.setData(report.winnerHp(), report.statuses());
        accelChart.setData(report.acceleration());
        strengthChart.setData(report.strength());
        updateStrengthKpis(report);
        updateStats(report);
        progressBar.setString(I18n.t("matchup.done"));
        statusLabel.setText(I18n.t("matchup.done"));
    }

    private void updateStats(MatchupAnalyzer.Report report) {
        avgFramesValue.setText(fmt(mean(report.frames(), report.statuses(), 0, 1, 2), "%.0f"));
        avgWinFramesValue.setText(fmt(mean(report.frames(), report.statuses(), 1), "%.0f"));
        avgLoseFramesValue.setText(fmt(mean(report.frames(), report.statuses(), 2), "%.0f"));
        avgWinHpValue.setText(fmt(mean(report.winnerHp(), report.statuses(), 1), "%.1f"));
        avgLoseHpValue.setText(fmt(mean(report.winnerHp(), report.statuses(), 2), "%.1f"));
    }

    private void resetStats() {
        avgFramesValue.setText("—");
        avgWinFramesValue.setText("—");
        avgLoseFramesValue.setText("—");
        avgWinHpValue.setText("—");
        avgLoseHpValue.setText("—");
    }

    /** 按状态过滤后求平均；无样本返回 NaN。 */
    private static double mean(int[] values, int[] statuses, int... accepted) {
        long sum = 0;
        int count = 0;
        int n = Math.min(values.length, statuses.length);
        for (int i = 0; i < n; i++) {
            for (int status : accepted) {
                if (statuses[i] == status) {
                    sum += values[i];
                    count++;
                    break;
                }
            }
        }
        return count > 0 ? (double) sum / count : Double.NaN;
    }

    private static String fmt(double value, String format) {
        return Double.isNaN(value) ? "—" : String.format(format, value);
    }

    private static Color upColor() {
        return Main.DARK_MODE ? new Color(0x81C784) : new Color(0x2E7D32);
    }

    private static Color downColor() {
        return Main.DARK_MODE ? new Color(0xE57373) : new Color(0xC62828);
    }

    /** NaN 视为最大：升序排列时排在最后（未遇到样本不足的行置底）。 */
    private static final Comparator<Double> NAN_LAST = (a, b) -> {
        boolean na = a.isNaN();
        boolean nb = b.isNaN();
        if (na || nb) {
            return na == nb ? 0 : (na ? 1 : -1);
        }
        return Double.compare(a, b);
    };

    /** 出现次数低于该值的行视为低样本，差值不可靠。 */
    private static final int LOW_SAMPLE_MIN = 3;

    private void installRenderers() {
        sensitivityTable.setRowSorter(sensitivitySorter);
        sensitivitySorter.setComparator(2, NAN_LAST);
        sensitivitySorter.setComparator(3, NAN_LAST);
        sensitivitySorter.setComparator(4, NAN_LAST);
        sensitivitySorter.setComparator(5, NAN_LAST);
        // 默认按修正差值升序：最吃亏的排最前，低样本单位已被收缩、不会占据榜首
        sensitivitySorter.setSortKeys(List.of(new RowSorter.SortKey(5, SortOrder.ASCENDING)));
        sensitivityTable.getColumnModel().getColumn(2).setCellRenderer(new PercentRenderer());
        sensitivityTable.getColumnModel().getColumn(3).setCellRenderer(new PercentRenderer());
        sensitivityTable.getColumnModel().getColumn(4).setCellRenderer(new SignedPercentRenderer());
        sensitivityTable.getColumnModel().getColumn(5).setCellRenderer(new SignedPercentRenderer());
    }

    /** 出现次数低于阈值的行淡化显示：保留原始信息，但提示其差值不可靠。 */
    private static boolean lowSample(JTable table, int viewRow) {
        if (table.getModel() instanceof SensitivityTableModel model) {
            return model.samplesAt(table.convertRowIndexToModel(viewRow)) < LOW_SAMPLE_MIN;
        }
        return false;
    }

    private static Color fadedColor(JTable table) {
        Color fg = table.getForeground();
        return new Color(fg.getRed(), fg.getGreen(), fg.getBlue(), 110);
    }

    /** 胜率：无正负号。 */
    private static final class PercentRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.RIGHT);
            if (value instanceof Double d) {
                setText(d.isNaN() ? "—" : String.format("%.1f%%", d));
            }
            if (!isSelected && lowSample(table, row)) {
                setForeground(fadedColor(table));
            }
            return this;
        }
    }

    /** 差值：带正负号与红绿配色（正=遇到该兵玉时我方更占优）。 */
    private static final class SignedPercentRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.RIGHT);
            if (value instanceof Double d) {
                if (d.isNaN()) {
                    setText("—");
                    if (!isSelected) {
                        setForeground(table.getForeground());
                    }
                } else {
                    setText(String.format("%+.1f%%", d));
                    if (!isSelected) {
                        setForeground(d > 0 ? upColor() : (d < 0 ? downColor() : table.getForeground()));
                    }
                }
            }
            if (!isSelected && lowSample(table, row)) {
                setForeground(fadedColor(table));
            }
            return this;
        }
    }

    /** 单位敏感度表模型。 */
    private static final class SensitivityTableModel extends AbstractTableModel {
        private static final String[] COLUMN_KEYS =
                {"matchup.col.unit", "matchup.col.samples", "matchup.col.withRate",
                 "matchup.col.withoutRate", "matchup.col.delta", "matchup.col.adjustedDelta"};
        private List<MatchupAnalyzer.SensitivityRow> rows = List.of();

        void setRows(List<MatchupAnalyzer.SensitivityRow> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        int samplesAt(int row) {
            return rows.get(row).samples();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_KEYS.length;
        }

        @Override
        public String getColumnName(int column) {
            return I18n.t(COLUMN_KEYS[column]);
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case 0 -> String.class;
                case 1 -> Integer.class;
                default -> Double.class;
            };
        }

        @Override
        public Object getValueAt(int row, int column) {
            MatchupAnalyzer.SensitivityRow r = rows.get(row);
            return switch (column) {
                case 0 -> I18n.unitName(r.type());
                case 1 -> r.samples();
                case 2 -> r.withRate();
                case 3 -> r.withoutRate();
                case 4 -> r.delta();
                default -> r.adjustedDelta();
            };
        }
    }

}
