package org.example.GUI;

import org.example.CompiledFort;
import org.example.ComboSelector;
import org.example.ContributionAnalyzer;
import org.example.Main;
import org.example.Setting;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;

/** 贡献分析标签页：批量对战找出对胜率贡献小的单位。 */
public class ContributionTab extends JPanel {

    // —— 输入 ——
    private final JTextField playerField = new JTextField();
    private final FixedJTextArea evalArea = new FixedJTextArea();
    private final JLabel playerInfoLabel = new JLabel(" ");
    private final JLabel evalInfoLabel = new JLabel(" ");

    // —— 参数 ——
    private final JSpinner exploreSpinner = new JSpinner(new SpinnerNumberModel(100, 0, 100, 5));
    private final JSpinner deleteSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));
    private final JSpinner threadSpinner = new JSpinner(
            new SpinnerNumberModel(Math.min(256, Math.max(1, Main.MAX_THREADS)), 1, 256, 1));
    private final JLabel scaleLabel = new JLabel(" ");
    private final JButton startButton = new JButton(org.example.I18n.t("contrib.start"));
    private final JButton stopButton = new JButton(org.example.I18n.t("contrib.stop"));

    // —— 进度 ——
    private final JProgressBar progressBar = new JProgressBar();
    private final JLabel statusLabel = new JLabel(org.example.I18n.t("contrib.ready"));
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
    private final JButton copyButton = new JButton(org.example.I18n.t("contrib.copy"));

    // —— 状态 ——
    private final Timer refreshTimer = new Timer(300, e -> refreshInputState());
    private boolean analyzing = false;
    private List<CompiledFort> parsedEval = List.of();
    private int deletableCount = 0;
    private ContributionAnalyzer.Report lastReport;
    private SwingWorker<ContributionAnalyzer.Report, Void> worker;
    private long startTime;
    private String analyzedPlayerText;
    private boolean overCap = false;
    private JButton importPlayerButton;
    private JButton import2pButton;
    private JButton import1pButton;
    private JButton clearEvalButton;

    public ContributionTab() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        refreshTimer.setRepeats(false);
        exploreSpinner.setToolTipText(org.example.I18n.t("contrib.exploreTip"));

        add(buildNorth(), BorderLayout.NORTH);
        add(buildResultPanel(), BorderLayout.CENTER);
        add(buildProgressPanel(), BorderLayout.SOUTH);

        wireEvents();
        installRenderers();
        refreshInputState();
    }

    /** 深色模式切换时由 MainGUI 调用。 */
    public void updateDarkMode() {
        repaint();
        comboTable.repaint();
        unitTable.repaint();
        updateScaleLabel();
    }

    private JPanel buildNorth() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(buildInputPanel());
        panel.add(buildParamPanel());
        return panel;
    }

    private JPanel buildInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setBorder(BorderFactory.createTitledBorder(org.example.I18n.t("contrib.input")));

        JPanel playerBox = new JPanel();
        playerBox.setLayout(new BoxLayout(playerBox, BoxLayout.Y_AXIS));
        JPanel playerRow = new JPanel(new BorderLayout(8, 0));
        playerRow.add(new JLabel(org.example.I18n.t("contrib.playerFort")), BorderLayout.WEST);
        playerField.setFont(org.example.I18n.font(Font.PLAIN, 13));
        playerRow.add(playerField, BorderLayout.CENTER);
        importPlayerButton = new JButton(org.example.I18n.t("contrib.importFrom1p"));
        importPlayerButton.addActionListener(e -> {
            String first = firstFormationText(BattleTab.readFileAutoEncoding("1P.txt"));
            if (first == null) {
                JOptionPane.showMessageDialog(this, org.example.I18n.t("contrib.noFortIn1p"), org.example.I18n.t("msg.tip"), JOptionPane.WARNING_MESSAGE);
            } else {
                playerField.setText(first);
            }
        });
        playerRow.add(importPlayerButton, BorderLayout.EAST);
        playerBox.add(playerRow);
        playerInfoLabel.setFont(org.example.I18n.font(Font.PLAIN, 12));
        playerBox.add(playerInfoLabel);
        panel.add(playerBox, BorderLayout.NORTH);

        JPanel evalBox = new JPanel(new BorderLayout(0, 2));
        evalBox.add(new JLabel(org.example.I18n.t("contrib.evalSet")), BorderLayout.NORTH);
        evalArea.setFont(org.example.I18n.font(Font.PLAIN, 13));
        evalArea.setRows(4);
        evalBox.add(new JScrollPane(evalArea), BorderLayout.CENTER);
        panel.add(evalBox, BorderLayout.CENTER);

        JPanel evalButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        import2pButton = new JButton(org.example.I18n.t("contrib.importFrom2p"));
        import2pButton.addActionListener(e -> evalArea.setText(BattleTab.readFileAutoEncoding("2P.txt")));
        import1pButton = new JButton(org.example.I18n.t("contrib.importFrom1p"));
        import1pButton.addActionListener(e -> evalArea.setText(BattleTab.readFileAutoEncoding("1P.txt")));
        clearEvalButton = new JButton(org.example.I18n.t("contrib.clear"));
        clearEvalButton.addActionListener(e -> evalArea.setText(""));
        evalButtons.add(import2pButton);
        evalButtons.add(import1pButton);
        evalButtons.add(clearEvalButton);
        evalInfoLabel.setFont(org.example.I18n.font(Font.PLAIN, 12));
        evalButtons.add(evalInfoLabel);
        panel.add(evalButtons, BorderLayout.SOUTH);

        panel.setPreferredSize(new Dimension(0, 190));
        return panel;
    }

    private JPanel buildParamPanel() {
        JPanel panel = new JPanel(new WrapLayout(FlowLayout.LEFT, 8, 4));
        panel.setBorder(BorderFactory.createTitledBorder(org.example.I18n.t("contrib.config")));
        panel.add(new JLabel(org.example.I18n.t("contrib.explore")));
        panel.add(exploreSpinner);
        panel.add(new JLabel("%"));
        panel.add(new JLabel(org.example.I18n.t("contrib.removeCount")));
        panel.add(deleteSpinner);
        panel.add(new JLabel(org.example.I18n.t("contrib.threads")));
        panel.add(threadSpinner);
        panel.add(startButton);
        panel.add(stopButton);
        stopButton.setEnabled(false);
        scaleLabel.setFont(org.example.I18n.font(Font.PLAIN, 12));
        panel.add(scaleLabel);
        return panel;
    }

    private JPanel buildResultPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setBorder(BorderFactory.createTitledBorder(org.example.I18n.t("contrib.result")));
        baselineLabel.setFont(org.example.I18n.font(Font.PLAIN, 13));
        panel.add(baselineLabel, BorderLayout.NORTH);

        resultTabs.addTab(org.example.I18n.t("contrib.unitSummary"), unitScroll);
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
        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                scheduleRefresh();
            }

            public void removeUpdate(DocumentEvent e) {
                scheduleRefresh();
            }

            public void changedUpdate(DocumentEvent e) {
                scheduleRefresh();
            }
        };
        playerField.getDocument().addDocumentListener(listener);
        evalArea.getDocument().addDocumentListener(listener);
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

    private void scheduleRefresh() {
        refreshTimer.restart();
    }

    private void refreshInputState() {
        if (analyzing) {
            return;
        }
        parsedEval = List.of();
        deletableCount = 0;

        String evalText = evalArea.getText().trim();
        String evalError = null;
        if (!evalText.isEmpty()) {
            try {
                parsedEval = Setting.parseForts(evalText);
            } catch (Exception ex) {
                evalError = org.example.I18n.t("contrib.evalParseFail") + ex.getMessage();
            }
        }
        if (evalError != null) {
            evalInfoLabel.setText(evalError);
        } else if (parsedEval.isEmpty()) {
            evalInfoLabel.setText(evalText.isEmpty() ? org.example.I18n.t("contrib.noEval") : org.example.I18n.t("contrib.noEvalParsed"));
        } else {
            evalInfoLabel.setText(org.example.I18n.t("contrib.evalParsed", parsedEval.size()));
        }

        String playerText = playerField.getText().trim();
        if (playerText.isEmpty()) {
            playerInfoLabel.setText(org.example.I18n.t("contrib.noPlayer"));
        } else {
            try {
                List<CompiledFort> forts = Setting.parseForts(playerText);
                if (forts.size() != 1) {
                    playerInfoLabel.setText(org.example.I18n.t("contrib.onlyOne"));
                } else {
                    Formation formation = Formation.decode(playerText);
                    deletableCount = formation.units.size() - 1;
                    int cost = 0;
                    for (int i = 1; i <= deletableCount; i++) {
                        int c = Unit.infos[formation.units.get(i).id].cost();
                        cost += Math.max(c, 1);
                    }
                    playerInfoLabel.setText(org.example.I18n.t("contrib.playerInfo",
                            formation.name.isEmpty() ? org.example.I18n.t("common.none") : formation.name,
                            deletableCount, cost));
                }
            } catch (Exception ex) {
                playerInfoLabel.setText(org.example.I18n.t("contrib.fortParseFail") + ex.getMessage());
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

    private void updateScaleLabel() {
        int n = (int) deleteSpinner.getValue();
        int m = (int) exploreSpinner.getValue();
        overCap = false;
        boolean ok = deletableCount > 0 && !parsedEval.isEmpty() && n >= 1 && n <= deletableCount;
        if (!ok) {
            scaleLabel.setText(" ");
            scaleLabel.setForeground(null);
        } else if (m == 0) {
            scaleLabel.setText(org.example.I18n.t("contrib.scaleZero"));
            scaleLabel.setForeground(null);
        } else {
            long total = ComboSelector.combinationCount(deletableCount, n);
            long k = ComboSelector.plannedCount(total, m);
            long battles = (k + 1) * parsedEval.size();
            overCap = k > ComboSelector.MAX_SAMPLES;
            if (overCap) {
                scaleLabel.setText(org.example.I18n.t("contrib.overCap",
                        k, ComboSelector.MAX_SAMPLES));
            } else {
                scaleLabel.setText(org.example.I18n.t("contrib.scaleInfo", total, k, battles));
            }
            scaleLabel.setForeground(overCap || battles > 2_000_000 ? warnColor() : null);
        }
        startButton.setEnabled(!analyzing && ok && !overCap);
    }

    private void startAnalysis() {
        if (analyzing) {
            return;
        }
        refreshInputState();
        if (parsedEval.isEmpty() || deletableCount < 1) {
            JOptionPane.showMessageDialog(this, org.example.I18n.t("contrib.needInput"), org.example.I18n.t("msg.tip"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (overCap) {
            JOptionPane.showMessageDialog(this,
                    org.example.I18n.t("contrib.overCapMsg", ComboSelector.MAX_SAMPLES),
                    org.example.I18n.t("msg.tip"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        String playerText = playerField.getText().trim();
        analyzedPlayerText = playerText;
        String evalText = evalArea.getText().trim();
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
        baselineLabel.setText(org.example.I18n.t("contrib.startBaseline"));
        progressBar.setMaximum(100);
        progressBar.setValue(0);
        progressBar.setString("0");
        statusLabel.setText(org.example.I18n.t("contrib.analyzing"));
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
                                    statusLabel.setText(org.example.I18n.t("contrib.analyzingPhase", phase));
                                    long elapsedMs = (System.nanoTime() - startTime) / 1_000_000L;
                                    String eta = (done > 0 && total > done)
                                            ? org.example.I18n.t("contrib.eta",
                                                    (total - done) * (elapsedMs / (double) done) / 1000.0)
                                            : (total > done ? org.example.I18n.t("contrib.etaEstimating") : org.example.I18n.t("contrib.etaSoon"));
                                    timeLabel.setText(org.example.I18n.t("contrib.elapsed", elapsedMs / 1000.0, eta));
                                });
                            }

                            @Override
                            public void onBaseline(ContributionAnalyzer.Baseline baseline) {
                                SwingUtilities.invokeLater(() -> baselineLabel.setText(baselineText(baseline) + org.example.I18n.t("contrib.comboComputing")));
                            }
                        }, this::isCancelled);
            }

            @Override
            protected void done() {
                analyzing = false;
                setControlsEnabled(true);
                try {
                    if (isCancelled()) {
                        statusLabel.setText(org.example.I18n.t("contrib.cancelled"));
                        progressBar.setString(org.example.I18n.t("contrib.cancelled"));
                        return;
                    }
                    ContributionAnalyzer.Report report = get();
                    if (report == null) {
                        statusLabel.setText(org.example.I18n.t("contrib.cancelled"));
                        progressBar.setString(org.example.I18n.t("contrib.cancelled"));
                        return;
                    }
                    showReport(report, params);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    statusLabel.setText(org.example.I18n.t("contrib.analyzeFail") + cause.getMessage());
                    JOptionPane.showMessageDialog(ContributionTab.this,
                            org.example.I18n.t("contrib.analyzeFail") + "\n" + cause.getMessage(), org.example.I18n.t("msg.error"), JOptionPane.ERROR_MESSAGE);
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
        playerField.setEnabled(enabled);
        evalArea.setEnabled(enabled);
        importPlayerButton.setEnabled(enabled);
        import2pButton.setEnabled(enabled);
        import1pButton.setEnabled(enabled);
        clearEvalButton.setEnabled(enabled);
        exploreSpinner.setEnabled(enabled);
        deleteSpinner.setEnabled(enabled);
        threadSpinner.setEnabled(enabled);
        stopButton.setEnabled(!enabled);
        if (enabled) {
            updateScaleLabel();
        } else {
            startButton.setEnabled(false);
        }
    }

    private void showReport(ContributionAnalyzer.Report report, ContributionAnalyzer.Params params) {
        lastReport = report;
        baselineLabel.setText(baselineText(report.baseline())
                + org.example.I18n.t("contrib.reportSampled",
                report.sampledCombos(), report.totalCombos(), report.elapsedMs() / 1000.0));
        comboModel.setRefs(report.refs());
        comboModel.setRows(report.combos());
        unitModel.setRows(report.units());
        setComboTabVisible(params.deleteCount() >= 2 && report.sampledCombos() > 0);
        applyDefaultSort(params);
        copyButton.setEnabled(false);
        progressBar.setString(org.example.I18n.t("contrib.done"));
        statusLabel.setText(org.example.I18n.t("contrib.analyzeDone"));
        long battles = (long) (report.sampledCombos() + 1) * parsedEval.size();
        if (report.sampledCombos() == 0) {
            statusLabel.setText(org.example.I18n.t("contrib.analyzeDoneBaseline"));
        } else if (battles > 0 && report.battleErrors() * 10L > battles) {
            statusLabel.setText(org.example.I18n.t("contrib.analyzeDoneErrors", report.battleErrors()));
        }
    }

    private void setComboTabVisible(boolean visible) {
        int idx = resultTabs.indexOfComponent(comboScroll);
        if (visible && idx < 0) {
            resultTabs.insertTab(org.example.I18n.t("contrib.comboRank"), null, comboScroll, null, 0);
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
        statusLabel.setText(org.example.I18n.t("contrib.copied"));
    }

    private void updateCopyButton() {
        boolean hasSelection = comboTable.getSelectedRow() >= 0 || unitTable.getSelectedRow() >= 0;
        copyButton.setEnabled(!analyzing && hasSelection && lastReport != null);
    }

    private static String baselineText(ContributionAnalyzer.Baseline b) {
        return org.example.I18n.t("contrib.baseline",
                fmtPercent(b.winRate()), b.win(), b.lose(), b.draw(), b.timeout());
    }

    private static String fmtPercent(double v) {
        return Double.isNaN(v) ? "—" : String.format("%.2f%%", v);
    }

    private static String firstFormationText(String content) {
        for (String part : content.split("/")) {
            part = part.trim();
            if (!part.isEmpty()) {
                return part;
            }
        }
        return null;
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
            return org.example.I18n.t(COLUMN_KEYS[column]);
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
                sb.append(org.example.I18n.unitName(ref.type())).append("(").append(ref.cost()).append(")@(")
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
            return org.example.I18n.t(COLUMN_KEYS[column]);
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
                case 0 -> org.example.I18n.unitName(u.unit().type()) + "@(" + (u.unit().x()-16) + "," + (u.unit().y()-20) + ")";
                case 1 -> u.unit().cost();
                case 2 -> u.avgDelta();
                default -> u.avgDeltaPerCost();
            };
        }
    }
}
