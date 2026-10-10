package org.example.GUI;

import org.example.ComboSelector;
import org.example.ContributionAnalyzer;
import org.example.I18n;
import org.example.Main;
import org.example.Setting;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 行为分析 → 贡献分析：批量对战找出对胜率贡献小的单位。
 *
 * <p>左侧为配置列（约占 1/4 宽），右侧为结果列，中间分隔线可拖动；底部为进度栏。
 * 输入（原始阵型 / 测试阵集）来自共享输入区，启动分析时同步读取，分析期间锁定输入区。
 */
public class ContributionAnalysisPanel extends BehaviorAnalysisPanel {

    // —— 配置 ——
    private final JSpinner exploreSpinner = new JSpinner(new SpinnerNumberModel(100, 0, 100, 5));
    private final JSpinner deleteSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
    private final JSpinner threadSpinner = new JSpinner(
            new SpinnerNumberModel(Math.min(256, Math.max(1, Main.MAX_THREADS)), 1, 256, 1));
    private final JTextArea scaleArea = new JTextArea();
    private final JButton startButton = new JButton(I18n.t("contrib.start"));
    private final JButton stopButton = new JButton(I18n.t("contrib.stop"));

    // —— 进度 ——
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel statusLabel = new JLabel(I18n.t("contrib.ready"));
    private final JLabel timeLabel = new JLabel(" ");

    // —— 结果 ——
    private final JLabel baselineLabel = new JLabel(" ");
    private final JTabbedPane resultTabs = new JTabbedPane();
    private final ComboTableModel comboModel = new ComboTableModel();
    private final UnitTableModel unitModel = new UnitTableModel();
    private final JTable comboTable = new JTable(comboModel);
    private final JTable unitTable = new JTable(unitModel);
    private final TableRowSorter<ComboTableModel> comboSorter = new TableRowSorter<>(comboModel);
    private final TableRowSorter<UnitTableModel> unitSorter = new TableRowSorter<>(unitModel);
    private final JScrollPane comboScroll = new JScrollPane(comboTable);
    private final JScrollPane unitScroll = new JScrollPane(unitTable);
    private final JButton copyButton = new JButton(I18n.t("contrib.copy"));

    // —— 状态 ——
    private boolean analyzing = false;
    private int evalCount = 0;        // 测试阵集解析出的阵型数
    private int deletableCount = 0;   // 原始阵型可删除的单位数（不含核心）
    private boolean overCap = false;
    private ContributionAnalyzer.Report lastReport;
    private SwingWorker<ContributionAnalyzer.Report, Void> worker;
    private long startTime;
    private String analyzedPlayerText;

    public ContributionAnalysisPanel(Supplier<BehaviorInput> inputs, Consumer<Boolean> busyListener) {
        super(inputs, busyListener);
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        exploreSpinner.setToolTipText(I18n.t("contrib.exploreTip"));

        add(buildSplit(), BorderLayout.CENTER);
        add(buildProgressPanel(), BorderLayout.SOUTH);

        wireEvents();
        installRenderers();
        updateScaleLabel();
    }

    @Override
    public String titleKey() {
        return "behavior.item.contribution";
    }

    @Override
    public void onInputChanged(BehaviorInput input) {
        refreshInputState(input);
    }

    @Override
    public void updateDarkMode() {
        repaint();
        comboTable.repaint();
        unitTable.repaint();
        updateScaleLabel();
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
        rows.add(paramRow(new JLabel(I18n.t("contrib.explore")), exploreSpinner, new JLabel("%")));
        rows.add(paramRow(new JLabel(I18n.t("contrib.removeCount")), deleteSpinner));
        rows.add(paramRow(new JLabel(I18n.t("contrib.threads")), threadSpinner));
        rows.add(paramRow(startButton, stopButton));
        stopButton.setEnabled(false);

        scaleArea.setEditable(false);
        scaleArea.setFocusable(false);
        scaleArea.setOpaque(false);
        scaleArea.setLineWrap(true);
        scaleArea.setWrapStyleWord(true);
        scaleArea.setFont(I18n.font(Font.PLAIN, 12));

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createTitledBorder(I18n.t("contrib.config")));
        // 显式设置下限：否则 JSplitPane 会按控件当前宽度推算最小值，导致分隔线无法继续向左拖动
        panel.setMinimumSize(new Dimension(180, 0));
        panel.add(rows, BorderLayout.NORTH);
        panel.add(scaleArea, BorderLayout.CENTER);
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
        panel.setBorder(BorderFactory.createTitledBorder(I18n.t("contrib.result")));
        baselineLabel.setFont(I18n.font(Font.PLAIN, 13));
        panel.add(baselineLabel, BorderLayout.NORTH);

        resultTabs.addTab(I18n.t("contrib.unitSummary"), unitScroll);
        panel.add(resultTabs, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        copyButton.setEnabled(false);
        bottom.add(copyButton);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
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
        exploreSpinner.addChangeListener(e -> updateScaleLabel());
        deleteSpinner.addChangeListener(e -> updateScaleLabel());
        startButton.addActionListener(e -> startAnalysis());
        stopButton.addActionListener(e -> stopAnalysis());
        copyButton.addActionListener(e -> copySelectedCombo());
        comboTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    copyFrom(comboTable);
                }
            }
        });
        unitTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    copyFrom(unitTable);
                }
            }
        });
        comboTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateCopyButton();
            }
        });
        unitTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateCopyButton();
            }
        });
    }

    /**
     * 按输入内容刷新删除数上限、测试阵集数量与启动按钮状态。
     * 分析进行中不刷新，保证本次分析的口径与启动时一致。
     */
    private void refreshInputState(BehaviorInput input) {
        if (analyzing) {
            return;
        }
        evalCount = countForts(input.evalText());
        deletableCount = 0;
        if (countForts(input.playerText()) == 1) {
            try {
                deletableCount = Formation.decode(input.playerText()).units.size() - 1;
            } catch (RuntimeException ex) {
                deletableCount = 0;
            }
        }

        SpinnerNumberModel model = (SpinnerNumberModel) deleteSpinner.getModel();
        int max = Math.max(1, deletableCount);
        model.setMaximum(max);
        if ((int) deleteSpinner.getValue() > max) {
            deleteSpinner.setValue(max);
        }
        updateScaleLabel();
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

    private void updateScaleLabel() {
        int n = (int) deleteSpinner.getValue();
        int m = (int) exploreSpinner.getValue();
        overCap = false;
        boolean ok = deletableCount > 0 && evalCount > 0 && n >= 1 && n <= deletableCount;
        Color color = null;
        if (!ok) {
            scaleArea.setText("");
        } else if (m == 0) {
            scaleArea.setText(I18n.t("contrib.scaleZero"));
        } else {
            long total = ComboSelector.combinationCount(deletableCount, n);
            long k = ComboSelector.plannedCount(total, m);
            long battles = (k + 1) * evalCount;
            overCap = k > ComboSelector.MAX_SAMPLES;
            if (overCap) {
                scaleArea.setText(I18n.t("contrib.overCap", k, ComboSelector.MAX_SAMPLES));
            } else {
                scaleArea.setText(I18n.t("contrib.scaleInfo", total, k, battles));
            }
            if (overCap || battles > 2_000_000) {
                color = warnColor();
            }
        }
        scaleArea.setForeground(color != null ? color : UIManager.getColor("Label.foreground"));
        startButton.setEnabled(!analyzing && ok && !overCap);
    }

    private void startAnalysis() {
        if (analyzing) {
            return;
        }
        BehaviorInput input = currentInput();
        refreshInputState(input);
        if (evalCount == 0 || deletableCount < 1) {
            JOptionPane.showMessageDialog(this, I18n.t("contrib.needInput"),
                    I18n.t("msg.tip"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (overCap) {
            JOptionPane.showMessageDialog(this,
                    I18n.t("contrib.overCapMsg", ComboSelector.MAX_SAMPLES),
                    I18n.t("msg.tip"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        String playerText = input.playerText();
        String evalText = input.evalText();
        analyzedPlayerText = playerText;
        ContributionAnalyzer.Params params = new ContributionAnalyzer.Params(
                (int) deleteSpinner.getValue(),
                (int) exploreSpinner.getValue(),
                (int) threadSpinner.getValue(),
                ContributionAnalyzer.SortKey.DELTA,
                ContributionAnalyzer.SortOrder.DESC);

        analyzing = true;
        setControlsEnabled(false);
        lastReport = null;
        comboTable.clearSelection();
        unitTable.clearSelection();
        copyButton.setEnabled(false);
        comboModel.setRows(List.of());
        unitModel.setRows(List.of());
        baselineLabel.setText(I18n.t("contrib.startBaseline"));
        progressBar.setMaximum(100);
        progressBar.setValue(0);
        progressBar.setString("0");
        statusLabel.setText(I18n.t("contrib.analyzing"));
        timeLabel.setText(" ");
        startTime = System.nanoTime();

        worker = new SwingWorker<>() {
            @Override
            protected ContributionAnalyzer.Report doInBackground() throws Exception {
                return ContributionAnalyzer.analyze(playerText, evalText, params,
                        new ContributionAnalyzer.ProgressListener() {
                            @Override
                            public void onProgress(int done, long total, String phase) {
                                SwingUtilities.invokeLater(() -> {
                                    if (!analyzing) {
                                        return;
                                    }
                                    progressBar.setMaximum((int) Math.min(total, Integer.MAX_VALUE));
                                    progressBar.setValue(done);
                                    progressBar.setString(done + " / " + total);
                                    statusLabel.setText(I18n.t("contrib.analyzingPhase", phase));
                                    long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
                                    String eta = (done > 0 && total > done)
                                            ? I18n.t("contrib.eta", (total - done) * (elapsedMs / (double) done) / 1000.0)
                                            : (total > done ? I18n.t("contrib.etaEstimating") : I18n.t("contrib.etaSoon"));
                                    timeLabel.setText(I18n.t("contrib.elapsed", elapsedMs / 1000.0, eta));
                                });
                            }

                            @Override
                            public void onBaseline(ContributionAnalyzer.Baseline baseline) {
                                SwingUtilities.invokeLater(() -> baselineLabel.setText(
                                        baselineText(baseline) + I18n.t("contrib.comboComputing")));
                            }
                        }, this::isCancelled);
            }

            @Override
            protected void done() {
                analyzing = false;
                setControlsEnabled(true);
                try {
                    if (isCancelled()) {
                        statusLabel.setText(I18n.t("contrib.cancelled"));
                        progressBar.setString(I18n.t("contrib.cancelled"));
                        return;
                    }
                    ContributionAnalyzer.Report report = get();
                    if (report == null) {
                        statusLabel.setText(I18n.t("contrib.cancelled"));
                        progressBar.setString(I18n.t("contrib.cancelled"));
                        return;
                    }
                    showReport(report, params);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    statusLabel.setText(I18n.t("contrib.analyzeFail") + cause.getMessage());
                    JOptionPane.showMessageDialog(ContributionAnalysisPanel.this,
                            I18n.t("contrib.analyzeFail") + "\n" + cause.getMessage(),
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

    /** 切换配置与输入区的可用状态；分析进行中同时锁定共享输入区。 */
    private void setControlsEnabled(boolean enabled) {
        exploreSpinner.setEnabled(enabled);
        deleteSpinner.setEnabled(enabled);
        threadSpinner.setEnabled(enabled);
        stopButton.setEnabled(!enabled);
        setInputBusy(!enabled);
        if (enabled) {
            updateScaleLabel();
        } else {
            startButton.setEnabled(false);
        }
    }

    private void showReport(ContributionAnalyzer.Report report, ContributionAnalyzer.Params params) {
        lastReport = report;
        baselineLabel.setText(baselineText(report.baseline())
                + I18n.t("contrib.reportSampled",
                report.sampledCombos(), report.totalCombos(), report.elapsedMs() / 1000.0));
        comboModel.setRefs(report.refs());
        comboModel.setRows(report.combos());
        unitModel.setRows(report.units());
        setComboTabVisible(params.deleteCount() >= 2 && report.sampledCombos() > 0);
        applyDefaultSort(params);
        copyButton.setEnabled(false);
        progressBar.setString(I18n.t("contrib.done"));
        statusLabel.setText(I18n.t("contrib.analyzeDone"));
        long battles = (long) (report.sampledCombos() + 1) * evalCount;
        if (report.sampledCombos() == 0) {
            statusLabel.setText(I18n.t("contrib.analyzeDoneBaseline"));
        } else if (battles > 0 && report.battleErrors() * 10L > battles) {
            statusLabel.setText(I18n.t("contrib.analyzeDoneErrors", report.battleErrors()));
        }
    }

    private void setComboTabVisible(boolean visible) {
        int idx = resultTabs.indexOfComponent(comboScroll);
        if (visible && idx < 0) {
            resultTabs.insertTab(I18n.t("contrib.comboRank"), null, comboScroll, null, 0);
        } else if (!visible && idx >= 0) {
            resultTabs.remove(idx);
        }
    }

    private void applyDefaultSort(ContributionAnalyzer.Params params) {
        boolean delta = params.sortKey() == ContributionAnalyzer.SortKey.DELTA;
        javax.swing.SortOrder order = params.order() == ContributionAnalyzer.SortOrder.DESC
                ? javax.swing.SortOrder.DESCENDING : javax.swing.SortOrder.ASCENDING;
        comboSorter.setSortKeys(List.of(new RowSorter.SortKey(
                delta ? COMBO_DELTA_COLUMN : COMBO_PER_COST_COLUMN, order)));
        unitSorter.setSortKeys(List.of(new RowSorter.SortKey(
                delta ? UNIT_DELTA_COLUMN : UNIT_PER_COST_COLUMN, order)));
    }

    private void copySelectedCombo() {
        if (comboTable.getSelectedRow() >= 0) {
            copyFrom(comboTable);
        } else {
            copyFrom(unitTable);
        }
    }

    private void copyFrom(JTable table) {
        if (lastReport == null || analyzedPlayerText == null) {
            return;
        }
        int[] unitIndices;
        int viewRow;
        if (table == comboTable) {
            viewRow = comboTable.getSelectedRow();
            if (viewRow < 0) {
                return;
            }
            unitIndices = comboModel.get(comboTable.convertRowIndexToModel(viewRow)).unitIndices();
        } else {
            viewRow = unitTable.getSelectedRow();
            if (viewRow < 0) {
                return;
            }
            unitIndices = new int[]{unitModel.get(unitTable.convertRowIndexToModel(viewRow)).unit().index()};
        }
        String code = ContributionAnalyzer.removeUnitsFromCode(analyzedPlayerText, unitIndices);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
        statusLabel.setText(I18n.t("contrib.copied"));
    }

    private void updateCopyButton() {
        boolean hasSelection = comboTable.getSelectedRow() >= 0 || unitTable.getSelectedRow() >= 0;
        copyButton.setEnabled(!analyzing && hasSelection && lastReport != null);
    }

    private static String baselineText(ContributionAnalyzer.Baseline b) {
        return I18n.t("contrib.baseline",
                fmtPercent(b.winRate()), b.win(), b.lose(), b.draw(), b.timeout());
    }

    private static String fmtPercent(double v) {
        return Double.isNaN(v) ? "—" : String.format("%.2f%%", v);
    }

    private static Color upColor() {
        return Main.DARK_MODE ? new Color(0x81C784) : new Color(0x2E7D32);
    }

    private static Color downColor() {
        return Main.DARK_MODE ? new Color(0xE57373) : new Color(0xC62828);
    }

    private static Color warnColor() {
        return Main.DARK_MODE ? new Color(0xFF8A80) : new Color(0xD32F2F);
    }

    private static final int COMBO_PLAIN_RATE_COLUMN = 2;
    private static final int COMBO_DELTA_COLUMN = 3;
    private static final int COMBO_PER_COST_COLUMN = 4;
    private static final int UNIT_DELTA_COLUMN = 2;
    private static final int UNIT_PER_COST_COLUMN = 3;

    /** 比较器视 NaN 为最小；TableRowSorter 对 DESCENDING 取反后 NaN 置底，ASCENDING 时置顶。 */
    private static final Comparator<Double> NAN_LAST_DESC = (a, b) -> {
        boolean na = a.isNaN();
        boolean nb = b.isNaN();
        if (na || nb) {
            return na == nb ? 0 : (na ? -1 : 1);
        }
        return Double.compare(a, b);
    };

    private void installRenderers() {
        comboTable.setTableHeader(new FullNameHeader(comboTable.getColumnModel()));
        unitTable.setTableHeader(new FullNameHeader(unitTable.getColumnModel()));
        comboTable.setRowSorter(comboSorter);
        unitTable.setRowSorter(unitSorter);

        SignedRenderer signed = new SignedRenderer();
        comboSorter.setComparator(COMBO_PLAIN_RATE_COLUMN, NAN_LAST_DESC);
        comboSorter.setComparator(COMBO_DELTA_COLUMN, NAN_LAST_DESC);
        comboSorter.setComparator(COMBO_PER_COST_COLUMN, NAN_LAST_DESC);
        unitSorter.setComparator(UNIT_DELTA_COLUMN, NAN_LAST_DESC);
        unitSorter.setComparator(UNIT_PER_COST_COLUMN, NAN_LAST_DESC);

        comboTable.getColumnModel().getColumn(COMBO_PLAIN_RATE_COLUMN).setCellRenderer(new PlainPercentRenderer());
        comboTable.getColumnModel().getColumn(COMBO_DELTA_COLUMN).setCellRenderer(signed);
        comboTable.getColumnModel().getColumn(COMBO_PER_COST_COLUMN).setCellRenderer(new PerCostRenderer());
        unitTable.getColumnModel().getColumn(UNIT_DELTA_COLUMN).setCellRenderer(signed);
        unitTable.getColumnModel().getColumn(UNIT_PER_COST_COLUMN).setCellRenderer(new PerCostRenderer());
    }

    /** 表头：结果列较窄时列名会被截断，鼠标悬停时显示完整列名。 */
    private static final class FullNameHeader extends JTableHeader {
        FullNameHeader(TableColumnModel model) {
            super(model);
            ToolTipManager.sharedInstance().registerComponent(this);
        }

        @Override
        public String getToolTipText(MouseEvent event) {
            int column = columnAtPoint(event.getPoint());
            if (column < 0) {
                return null;
            }
            Object value = getColumnModel().getColumn(column).getHeaderValue();
            return value == null ? null : value.toString();
        }
    }

    /** 带正负号与红绿配色的百分比渲染器。 */
    private static final class SignedRenderer extends DefaultTableCellRenderer {
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
                    setText(String.format("%+.2f%%", d));
                    if (!isSelected) {
                        setForeground(d > 0 ? upColor() : (d < 0 ? downColor() : table.getForeground()));
                    }
                }
            }
            return this;
        }
    }

    /** 删除后胜率：无正号。 */
    private static final class PlainPercentRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.RIGHT);
            if (value instanceof Double d) {
                setText(d.isNaN() ? "—" : String.format("%.2f%%", d));
            }
            return this;
        }
    }

    /** 每费变化：%+/费，带红绿配色。 */
    private static final class PerCostRenderer extends DefaultTableCellRenderer {
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
                    setText(String.format("%+.3f%%/￥", d));
                    if (!isSelected) {
                        setForeground(d > 0 ? upColor() : (d < 0 ? downColor() : table.getForeground()));
                    }
                }
            }
            return this;
        }
    }

    /** 组合排行表模型。 */
    private static final class ComboTableModel extends AbstractTableModel {
        private static final String[] COLUMN_KEYS =
                {"contrib.col.removeUnits", "contrib.col.removedCost", "contrib.col.newWinRate",
                 "contrib.col.delta", "contrib.col.perCost"};
        private List<ContributionAnalyzer.ComboResult> rows = List.of();
        private List<ContributionAnalyzer.UnitRef> refs = List.of();

        void setRows(List<ContributionAnalyzer.ComboResult> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        void setRefs(List<ContributionAnalyzer.UnitRef> refs) {
            this.refs = refs;
        }

        ContributionAnalyzer.ComboResult get(int row) {
            return rows.get(row);
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
            ContributionAnalyzer.ComboResult r = rows.get(row);
            return switch (column) {
                case 0 -> describe(r);
                case 1 -> (int) r.removedCost();
                case 2 -> r.winRate();
                case 3 -> r.delta();
                default -> r.deltaPerCost();
            };
        }

        private String describe(ContributionAnalyzer.ComboResult r) {
            StringBuilder sb = new StringBuilder();
            for (int idx : r.unitIndices()) {
                if (sb.length() > 0) {
                    sb.append(" + ");
                }
                ContributionAnalyzer.UnitRef ref = refs.get(idx);
                sb.append(I18n.unitName(ref.type())).append("(").append(ref.cost()).append(")@(")
                        .append(ref.x()).append(",").append(ref.y()).append(")");
            }
            return sb.toString();
        }
    }

    /** 单位汇总表模型。 */
    private static final class UnitTableModel extends AbstractTableModel {
        private static final String[] COLUMN_KEYS =
                {"contrib.col.unit", "contrib.col.cost", "contrib.col.avgDelta", "contrib.col.avgPerCost"};
        private List<ContributionAnalyzer.UnitSummary> rows = List.of();

        void setRows(List<ContributionAnalyzer.UnitSummary> rows) {
            this.rows = rows;
            fireTableDataChanged();
        }

        ContributionAnalyzer.UnitSummary get(int row) {
            return rows.get(row);
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
            ContributionAnalyzer.UnitSummary u = rows.get(row);
            return switch (column) {
                case 0 -> I18n.unitName(u.unit().type()) + "@(" + (u.unit().x() - 16) + "," + (u.unit().y() - 20) + ")";
                case 1 -> u.unit().cost();
                case 2 -> u.avgDelta();
                default -> u.avgDeltaPerCost();
            };
        }
    }
}
