package org.example.GUI;

import org.example.BattlefieldAnalyzer;
import org.example.I18n;
import org.example.Main;
import org.example.Setting;
import org.example.rust.RustBattle;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 行为分析 → 战场空间：把「原始阵型 × 测试阵集」的遥测事件归到我方阵型槽位，
 * 展示阵型受创图（气泡/平滑可切换）、推进曲线与开局压迫。
 *
 * <p>两阶段：先跑结果并按筛选条件取子集，再对子集运行 Rust 遥测；需要 Rust 模拟器。
 */
public class BattlefieldPanel extends BehaviorAnalysisPanel {

    private static final BattlefieldAnalyzer.Filter[] FILTERS = {
            BattlefieldAnalyzer.Filter.ALL, BattlefieldAnalyzer.Filter.WIN,
            BattlefieldAnalyzer.Filter.LOSE, BattlefieldAnalyzer.Filter.DRAW,
            BattlefieldAnalyzer.Filter.TIMEOUT};

    private static final SlotHeatmapPanel.Metric[] METRICS = {
            SlotHeatmapPanel.Metric.DAMAGE, SlotHeatmapPanel.Metric.DEATHS};

    private static final SlotHeatmapPanel.Render[] RENDERS = {
            SlotHeatmapPanel.Render.BUBBLE, SlotHeatmapPanel.Render.SMOOTH};

    private static final BattlefieldCharts.TimelineMetric[] TIMELINE_METRICS = {
            BattlefieldCharts.TimelineMetric.HP1, BattlefieldCharts.TimelineMetric.HP2,
            BattlefieldCharts.TimelineMetric.ALIVE1, BattlefieldCharts.TimelineMetric.ALIVE2,
            BattlefieldCharts.TimelineMetric.ATK1, BattlefieldCharts.TimelineMetric.ATK2};

    // —— 配置 ——
    private final JSpinner threadSpinner = new JSpinner(
            new SpinnerNumberModel(Math.min(256, Math.max(1, Main.MAX_THREADS)), 1, 256, 1));
    private final JSpinner binSpinner = new JSpinner(new SpinnerNumberModel(256, 64, 2048, 64));
    private final JComboBox<String> filterCombo = new JComboBox<>(new String[]{
            I18n.t("battlefield.filter.all"), I18n.t("battlefield.filter.win"),
            I18n.t("battlefield.filter.lose"), I18n.t("battlefield.filter.draw"),
            I18n.t("battlefield.filter.timeout")});
    private final JTextArea infoArea = new JTextArea();
    private final JButton startButton = new JButton(I18n.t("battlefield.start"));
    private final JButton stopButton = new JButton(I18n.t("battlefield.stop"));

    // —— 进度 ——
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel statusLabel = new JLabel(I18n.t("battlefield.ready"));
    private final JLabel timeLabel = new JLabel(" ");

    // —— 结果 ——
    private final JLabel summaryLabel = new JLabel(" ");
    private final JTabbedPane resultTabs = new JTabbedPane();
    private final SlotHeatmapPanel slotHeatmap = new SlotHeatmapPanel();
    private final BattlefieldCharts.TimelineChart timelineChart = new BattlefieldCharts.TimelineChart();
    private final BattlefieldCharts.DistributionChart firstCollisionChart =
            new BattlefieldCharts.DistributionChart("battlefield.pressure.firstCollision");
    private final BattlefieldCharts.DistributionChart firstCoreChart =
            new BattlefieldCharts.DistributionChart("battlefield.pressure.firstCore");
    private final JComboBox<String> slotMetricCombo = new JComboBox<>(new String[]{
            I18n.t("battlefield.metric.damage"), I18n.t("battlefield.metric.deaths")});
    private final JComboBox<String> slotRenderCombo = new JComboBox<>(new String[]{
            I18n.t("battlefield.render.bubble"), I18n.t("battlefield.render.smooth")});
    private final JComboBox<String> timelineMetricCombo = new JComboBox<>(new String[]{
            I18n.t("battlefield.tl.hp1"), I18n.t("battlefield.tl.hp2"),
            I18n.t("battlefield.tl.alive1"), I18n.t("battlefield.tl.alive2"),
            I18n.t("battlefield.tl.atk1"), I18n.t("battlefield.tl.atk2")});
    /** 推进曲线系列勾选框（0=平均，1=胜，2=负，3=其他）。 */
    private final JCheckBox[] timelineSeriesBoxes = new JCheckBox[BattlefieldCharts.SERIES_COUNT];

    // —— 状态 ——
    private boolean analyzing = false;
    private int evalCount = 0;
    private boolean playerReady = false;
    private SwingWorker<BattlefieldAnalyzer.Report, Void> worker;
    private long startTime;

    public BattlefieldPanel(Supplier<BehaviorInput> inputs, Consumer<Boolean> busyListener) {
        super(inputs, busyListener);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        threadSpinner.setToolTipText(I18n.t("matchup.threadsTip"));
        binSpinner.setToolTipText(I18n.t("battlefield.binFramesTip"));

        add(buildSplit(), BorderLayout.CENTER);
        add(buildProgressPanel(), BorderLayout.SOUTH);

        wireEvents();
        updateStartState();
    }

    @Override
    public String titleKey() {
        return "behavior.item.battlefield";
    }

    @Override
    public void onInputChanged(BehaviorInput input) {
        refreshInputState(input);
    }

    @Override
    public void updateDarkMode() {
        repaint();
        slotHeatmap.repaint();
        timelineChart.repaint();
        firstCollisionChart.repaint();
        firstCoreChart.repaint();
    }

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
        rows.add(paramRow(new JLabel(I18n.t("battlefield.threads")), threadSpinner));
        rows.add(paramRow(new JLabel(I18n.t("battlefield.binFrames")), binSpinner));
        rows.add(paramRow(new JLabel(I18n.t("battlefield.filter")), filterCombo));
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

        resultTabs.addTab(I18n.t("battlefield.tab.slots"), buildSlotsTab());
        resultTabs.addTab(I18n.t("battlefield.tab.timeline"), buildTimelineTab());
        resultTabs.addTab(I18n.t("battlefield.tab.pressure"), buildPressureTab());
        panel.add(resultTabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSlotsTab() {
        slotMetricCombo.addActionListener(e ->
                slotHeatmap.setMetric(METRICS[slotMetricCombo.getSelectedIndex()]));
        slotRenderCombo.addActionListener(e ->
                slotHeatmap.setRender(RENDERS[slotRenderCombo.getSelectedIndex()]));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        toolbar.add(new JLabel(I18n.t("battlefield.metric")));
        toolbar.add(slotMetricCombo);
        toolbar.add(new JLabel(I18n.t("battlefield.render")));
        toolbar.add(slotRenderCombo);

        JPanel tab = new JPanel(new BorderLayout(0, 4));
        tab.add(toolbar, BorderLayout.NORTH);
        tab.add(slotHeatmap, BorderLayout.CENTER);
        return tab;
    }

    private JPanel buildTimelineTab() {
        timelineMetricCombo.addActionListener(e ->
                timelineChart.setMetric(TIMELINE_METRICS[timelineMetricCombo.getSelectedIndex()]));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        toolbar.add(new JLabel(I18n.t("battlefield.metric")));
        toolbar.add(timelineMetricCombo);
        toolbar.add(new JLabel(I18n.t("battlefield.tl.show")));
        for (int i = 0; i < BattlefieldCharts.SERIES_COUNT; i++) {
            JCheckBox box = new JCheckBox(BattlefieldCharts.seriesLabel(i), seriesIcon(i), i != 3);
            final int series = i;
            box.addActionListener(e -> timelineChart.setSeriesVisible(series, box.isSelected()));
            timelineSeriesBoxes[i] = box;
            toolbar.add(box);
        }

        JPanel tab = new JPanel(new BorderLayout(0, 4));
        tab.add(toolbar, BorderLayout.NORTH);
        tab.add(timelineChart, BorderLayout.CENTER);
        return tab;
    }

    /** 系列色块图标（颜色随深色模式在绘制时取）。 */
    private static Icon seriesIcon(int series) {
        return new Icon() {
            @Override
            public int getIconWidth() {
                return 10;
            }

            @Override
            public int getIconHeight() {
                return 10;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                g.setColor(BattlefieldCharts.seriesColor(series));
                g.fillRect(x, y, 10, 10);
            }
        };
    }

    private JPanel buildPressureTab() {
        JPanel tab = new JPanel(new GridLayout(2, 1, 6, 6));
        tab.add(firstCollisionChart);
        tab.add(firstCoreChart);
        return tab;
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
    }

    private void refreshInputState(BehaviorInput input) {
        if (analyzing) {
            return;
        }
        evalCount = countForts(input.evalText());
        playerReady = countForts(input.playerText()) == 1;
        updateStartState();
    }

    private void updateStartState() {
        boolean rustReady = RustBattle.available();
        boolean ok = rustReady && playerReady && evalCount > 0;
        startButton.setEnabled(!analyzing && ok);
        if (!rustReady) {
            infoArea.setText(I18n.t("battlefield.err.needRustShort"));
        } else if (ok) {
            infoArea.setText(I18n.t("battlefield.scaleInfo", evalCount, evalCount));
        } else {
            infoArea.setText("");
        }
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
        if (!RustBattle.available()) {
            JOptionPane.showMessageDialog(this, I18n.t("battlefield.err.needRust"),
                    I18n.t("msg.tip"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!playerReady || evalCount == 0) {
            JOptionPane.showMessageDialog(this, I18n.t("matchup.needInput"),
                    I18n.t("msg.tip"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        String playerText = input.playerText();
        String evalText = input.evalText();
        int threads = (int) threadSpinner.getValue();
        int binFrames = (int) binSpinner.getValue();
        BattlefieldAnalyzer.Filter filter = FILTERS[filterCombo.getSelectedIndex()];
        String filterLabel = (String) filterCombo.getSelectedItem();

        analyzing = true;
        setControlsEnabled(false);
        summaryLabel.setText(" ");
        slotHeatmap.setData(List.of(), new double[0], new double[0]);
        timelineChart.setReport(null);
        firstCollisionChart.setData(new double[0], 0, new Color(0xFFB74D));
        firstCoreChart.setData(new double[0], 0, new Color(0xFFB74D));
        progressBar.setMaximum(Math.max(1, evalCount));
        progressBar.setValue(0);
        progressBar.setString("0 / " + evalCount);
        statusLabel.setText(I18n.t("battlefield.analyzing"));
        timeLabel.setText(" ");
        startTime = System.nanoTime();

        worker = new SwingWorker<>() {
            @Override
            protected BattlefieldAnalyzer.Report doInBackground() throws Exception {
                return BattlefieldAnalyzer.analyze(playerText, evalText, filter, threads, binFrames,
                        (phase, done, total) -> SwingUtilities.invokeLater(() -> {
                            if (!analyzing) {
                                return;
                            }
                            progressBar.setMaximum(Math.max(1, total));
                            progressBar.setValue(done);
                            progressBar.setString(done + " / " + total);
                            String phaseName = I18n.t(phase == 0
                                    ? "battlefield.phaseResults" : "battlefield.phaseTelemetry");
                            statusLabel.setText(I18n.t("battlefield.analyzingPhase", phaseName));
                            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
                            timeLabel.setText(I18n.t("battlefield.elapsed", elapsedMs / 1000.0));
                        }), this::isCancelled);
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
                    BattlefieldAnalyzer.Report report = get();
                    if (report == null) {
                        statusLabel.setText(I18n.t("matchup.cancelled"));
                        progressBar.setString(I18n.t("matchup.cancelled"));
                        return;
                    }
                    showReport(report, filterLabel);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    statusLabel.setText(I18n.t("matchup.fail") + cause.getMessage());
                    JOptionPane.showMessageDialog(BattlefieldPanel.this,
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
        binSpinner.setEnabled(enabled);
        filterCombo.setEnabled(enabled);
        stopButton.setEnabled(!enabled);
        setInputBusy(!enabled);
        if (enabled) {
            updateStartState();
        } else {
            startButton.setEnabled(false);
        }
    }

    private void showReport(BattlefieldAnalyzer.Report report, String filterLabel) {
        summaryLabel.setText(I18n.t("battlefield.summary", report.games(), filterLabel,
                report.elapsedMs() / 1000.0));
        slotHeatmap.setData(report.metas(), report.slotDamage(), report.slotDeaths());
        timelineChart.setReport(report);

        List<Double> collisions = new ArrayList<>();
        List<Double> ourCore = new ArrayList<>();
        List<Double> enemyCore = new ArrayList<>();
        for (int i = 0; i < report.games(); i++) {
            double collision = report.scalar(i, BattlefieldAnalyzer.S_FIRST_COLLISION);
            if (collision >= 0) {
                collisions.add(collision);
            }
            double firstOur = report.scalar(i, BattlefieldAnalyzer.S_FIRST_P1_CORE);
            if (firstOur >= 0) {
                ourCore.add(firstOur);
            }
            double firstEnemy = report.scalar(i, BattlefieldAnalyzer.S_FIRST_P2_CORE);
            if (firstEnemy >= 0) {
                enemyCore.add(firstEnemy);
            }
        }
        firstCollisionChart.setData(toArray(collisions), report.games() - collisions.size(),
                new Color(0xFFB74D));
        firstCoreChart.setData(toArray(ourCore), report.games() - ourCore.size(),
                new Color(0xE57373), "battlefield.pressure.ourCore",
                toArray(enemyCore), report.games() - enemyCore.size(),
                new Color(0x81C784), "battlefield.pressure.enemyCore");

        progressBar.setString(I18n.t("matchup.done"));
        statusLabel.setText(I18n.t("matchup.done"));
    }

    private static double[] toArray(List<Double> list) {
        double[] out = new double[list.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = list.get(i);
        }
        return out;
    }
}
