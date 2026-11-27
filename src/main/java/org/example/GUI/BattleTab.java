package org.example.GUI;

import org.example.Main;
import org.example.Setting;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.List;

/** 对战控制标签页：设置面板 + 阵容编辑 + 自动保存 + 批量模拟。 */
public class BattleTab extends JPanel {

    private final JFormattedTextField frameLimitField;
    private final JSpinner threadSpinner;
    private final JCheckBox showHpCheck;
    private final JCheckBox wordWrapCheck;
    private final JButton battleButton;
    private final JButton watchButton;
    private final JLabel engineLabel;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JLabel timeLabel;

    private final FixedJTextArea p1TextArea;
    private final FixedJTextArea p2TextArea;
    private final FixedJTextArea resultTextArea;
    private final FixedJTextArea simpleResultTextArea;

    private final Timer p1SaveTimer;
    private final Timer p2SaveTimer;
    private boolean loadingP1;
    private boolean loadingP2;

    private long battleStartTime;

    public BattleTab() {
        super(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // --- 设置栏 ---
        // 左侧设置项用 WrapLayout：窗口变窄时自动换行（FlowLayout 的首选高度恒为单行，多出的控件会被裁掉）；
        // 观战按钮固定在最右侧，垂直居中
        JPanel settingsPanel = new JPanel(new BorderLayout(8, 0));
        settingsPanel.setBorder(BorderFactory.createTitledBorder(org.example.I18n.t("battle.settings")));
        JPanel settingsFlow = new JPanel(new WrapLayout(FlowLayout.LEFT, 16, 4));
        settingsPanel.add(settingsFlow, BorderLayout.CENTER);

        settingsFlow.add(new JLabel(org.example.I18n.t("battle.frameLimit")));
        NumberFormat fmt = NumberFormat.getIntegerInstance();
        fmt.setGroupingUsed(false);
        NumberFormatter nf = new NumberFormatter(fmt);
        nf.setValueClass(Integer.class);
        nf.setMinimum(0);
        nf.setMaximum(Integer.MAX_VALUE);
        frameLimitField = new JFormattedTextField(nf);
        frameLimitField.setPreferredSize(new Dimension(80, 22));
        settingsFlow.add(frameLimitField);

        settingsFlow.add(new JLabel(org.example.I18n.t("battle.threads")));
        int cores = Runtime.getRuntime().availableProcessors();
        threadSpinner = new JSpinner(new SpinnerNumberModel(cores, 1, 256, 1));
        threadSpinner.setPreferredSize(new Dimension(60, 22));
        settingsFlow.add(threadSpinner);

        JLabel coresLabel = new JLabel(org.example.I18n.t("battle.cores", cores));
        settingsFlow.add(coresLabel);

        showHpCheck = new JCheckBox(org.example.I18n.t("battle.hpScore"));
        settingsFlow.add(showHpCheck);

        wordWrapCheck = new JCheckBox(org.example.I18n.t("battle.wordWrap"));
        settingsFlow.add(wordWrapCheck);

        battleButton = new JButton(org.example.I18n.t("battle.start"));
        settingsFlow.add(battleButton);

        engineLabel = new JLabel();
        refreshEngineLabel();
        settingsFlow.add(engineLabel);

        watchButton = new JButton(org.example.I18n.t("battle.watch"));
        watchButton.setToolTipText(org.example.I18n.t("battle.watchTip"));
        JPanel watchHolder = new JPanel(new GridBagLayout());
        watchHolder.add(watchButton);
        settingsPanel.add(watchHolder, BorderLayout.EAST);

        add(settingsPanel, BorderLayout.NORTH);

        // --- 文本区域 ---
        p1TextArea = createTextArea(true);
        p2TextArea = createTextArea(true);
        resultTextArea = createTextArea(false);
        simpleResultTextArea = createTextArea(false);

        p1SaveTimer = new Timer(500, e -> saveFile("1P.txt", p1TextArea));
        p1SaveTimer.setRepeats(false);
        p2SaveTimer = new Timer(500, e -> saveFile("2P.txt", p2TextArea));
        p2SaveTimer.setRepeats(false);

        addAutoSave(p1TextArea, p1SaveTimer, () -> loadingP1);
        addAutoSave(p2TextArea, p2SaveTimer, () -> loadingP2);

        JTabbedPane playerTabs = new JTabbedPane();
        playerTabs.addTab(org.example.I18n.t("battle.p1"), new JScrollPane(p1TextArea));
        playerTabs.addTab(org.example.I18n.t("battle.p2"), new JScrollPane(p2TextArea));

        JTabbedPane resultTabs = new JTabbedPane();
        resultTabs.addTab(org.example.I18n.t("battle.detail"), new JScrollPane(resultTextArea));
        resultTabs.addTab(org.example.I18n.t("battle.simple"), new JScrollPane(simpleResultTextArea));

        JSplitPane subTabs = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, playerTabs, resultTabs);
        subTabs.setResizeWeight(0.5);
        add(subTabs, BorderLayout.CENTER);

        // --- 进度栏 ---
        JPanel progressPanel = new JPanel(new BorderLayout(8, 4));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressPanel.add(progressBar, BorderLayout.NORTH);

        JPanel statusRow = new JPanel(new BorderLayout());
        statusLabel = new JLabel(org.example.I18n.t("battle.ready"));
        timeLabel = new JLabel("");
        statusRow.add(statusLabel, BorderLayout.WEST);
        statusRow.add(timeLabel, BorderLayout.EAST);
        progressPanel.add(statusRow, BorderLayout.SOUTH);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        add(progressPanel, BorderLayout.SOUTH);

        // --- 初始化（必须在监听注册之前，否则会触发 applySettings 用默认值覆盖 config） ---
        frameLimitField.setValue(Main.MAX_FRAME_LIMIT);
        threadSpinner.setValue(Main.MAX_THREADS);
        showHpCheck.setSelected(Main.SHOW_REMAIN_HP);
        wordWrapCheck.setSelected(Main.WORD_WRAP);

        // --- 监听 ---
        frameLimitField.addPropertyChangeListener("value", e -> applySettings());
        threadSpinner.addChangeListener(e -> applySettings());
        showHpCheck.addChangeListener(e -> applySettings());
        wordWrapCheck.addChangeListener(e -> applySettings());
        battleButton.addActionListener(e -> startBattle());
        watchButton.addActionListener(e -> openWatchWindow());

        loadEditorContent();
        refreshResultTabs();
    }

    private void applySettings() {
        Object val = frameLimitField.getValue();
        if (val instanceof Number) {
            Main.MAX_FRAME_LIMIT = ((Number) val).intValue();
        }
        Main.MAX_THREADS = (int) threadSpinner.getValue();
        Main.SHOW_REMAIN_HP = showHpCheck.isSelected();
        boolean prevWrap = Main.WORD_WRAP;
        Main.WORD_WRAP = wordWrapCheck.isSelected();
        if (Main.WORD_WRAP != prevWrap) {
            applyWordWrap();
        }
        Setting.saveConfig();

        if (Main.pool != null && !Main.pool.isShutdown()) {
            Main.pool.shutdown();
        }
        Main.pool = java.util.concurrent.Executors.newFixedThreadPool(Main.MAX_THREADS);
    }

    private void startBattle() {
        applySettings();
        // 先同步落盘编辑器内容，避免 500ms 自动保存防抖导致本轮读取到旧阵型
        p1SaveTimer.stop();
        p2SaveTimer.stop();
        saveFile("1P.txt", p1TextArea);
        saveFile("2P.txt", p2TextArea);
        setButtonsEnabled(false);
        progressBar.setValue(0);
        statusLabel.setText(org.example.I18n.t("battle.simulating"));
        timeLabel.setText("");
        battleStartTime = System.nanoTime();

        SwingWorker<List<Main.FortStats>, Integer> worker = new SwingWorker<>() {
            @Override
            protected List<Main.FortStats> doInBackground() {
                Main.p1Raws = Setting.ReadFortsRaw("1P.txt");
                Main.p2Raws = Setting.ReadFortsRaw("2P.txt");
                Main.p1List = Setting.compileAll(Main.p1Raws);
                Main.p2List = Setting.compileAll(Main.p2Raws);
                return Main.runAllBattles(this::publish);
            }

            @Override
            protected void process(List<Integer> chunks) {
                int latest = chunks.getLast();
                int total = (Main.p1List != null ? Main.p1List.size() : 1)
                          * (Main.p2List != null ? Main.p2List.size() : 1);
                if (total > 0) {
                    int pct = latest * 100 / total;
                    progressBar.setValue(pct);
                    progressBar.setString(latest + " / " + total);
                }
                long elapsed = (System.nanoTime() - battleStartTime) / 1_000_000L;
                timeLabel.setText(org.example.I18n.t("battle.elapsed", elapsed / 1000.0));
            }

            @Override
            protected void done() {
                try {
                    List<Main.FortStats> stats = get();
                    refreshResultTabs();
                    long elapsed = (System.nanoTime() - battleStartTime) / 1_000_000L;
                    progressBar.setValue(100);
                    progressBar.setString(org.example.I18n.t("battle.done"));

                    int p1Count = Main.p1List != null ? Main.p1List.size() : 0;
                    int p2Count = Main.p2List != null ? Main.p2List.size() : 0;
                    int totalWins = 0, totalLosses = 0, totalDraws = 0;
                    if (stats != null) {
                        for (Main.FortStats s : stats) {
                            totalWins += s.win;
                            totalLosses += s.lose;
                            totalDraws += s.draw;
                        }
                    }
                    statusLabel.setText(org.example.I18n.t("battle.summary",
                            p1Count, p2Count, p1Count * p2Count,
                            totalWins, totalLosses, totalDraws, elapsed / 1000.0));
                } catch (Exception e) {
                    statusLabel.setText(org.example.I18n.t("battle.error") + e.getMessage());
                    e.printStackTrace();
                }
                setButtonsEnabled(true);
            }
        };
        worker.execute();
    }

    /**
     * 打开 Rust 渲染器观战窗口：带入当前 1P/2P 编辑器内容（默认首个阵型对阵，
     * 其余阵型可在观战窗口内切换）与当前帧上限。
     */
    private void openWatchWindow() {
        applySettings();
        org.example.rust.RenderLauncher.Location location = org.example.rust.RenderLauncher.locate();
        if (location.path() == null) {
            JOptionPane.showMessageDialog(this,
                    org.example.I18n.t("battle.watchNotFound",
                            String.join("\n", location.attempts())),
                    org.example.I18n.t("battle.watch"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<org.example.Fort> p1 = Setting.parseFortsRaw(p1TextArea.getText());
        List<org.example.Fort> p2 = Setting.parseFortsRaw(p2TextArea.getText());
        if (p1.isEmpty() || p2.isEmpty()) {
            JOptionPane.showMessageDialog(this, org.example.I18n.t("battle.watchNoForts"),
                    org.example.I18n.t("battle.watch"), JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            org.example.rust.RenderLauncher.launch(location.path(), p1, p2, Main.MAX_FRAME_LIMIT,
                    (code, log) -> SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(this,
                                    org.example.I18n.t("battle.watchCrashed", code,
                                            log.isEmpty() ? "-" : log),
                                    org.example.I18n.t("battle.watch"),
                                    JOptionPane.ERROR_MESSAGE)));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    org.example.I18n.t("battle.watchFailed", ex.getMessage()),
                    org.example.I18n.t("battle.watch"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshEngineLabel() {
        if (org.example.rust.RustBattle.available()) {
            engineLabel.setText(org.example.I18n.t("battle.engineRust"));
            engineLabel.setToolTipText(org.example.I18n.t("battle.engineTipRust"));
        } else {
            engineLabel.setText(org.example.I18n.t("battle.engineJava"));
            engineLabel.setToolTipText(org.example.I18n.t("battle.engineTipJava",
                    org.example.rust.RustBattle.unavailableReason()));
        }
    }

    private void setButtonsEnabled(boolean enabled) {
        battleButton.setEnabled(enabled);
    }

    private void addAutoSave(FixedJTextArea area, Timer timer, java.util.function.BooleanSupplier loadingFlag) {
        area.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                if (!loadingFlag.getAsBoolean()) timer.restart();
            }
            public void removeUpdate(DocumentEvent e) {
                if (!loadingFlag.getAsBoolean()) timer.restart();
            }
            public void changedUpdate(DocumentEvent e) {}
        });
    }

    private void loadEditorContent() {
        loadingP1 = true;
        p1TextArea.setText(readFileAutoEncoding("1P.txt"));
        p1TextArea.setCaretPosition(0);
        loadingP1 = false;

        loadingP2 = true;
        p2TextArea.setText(readFileAutoEncoding("2P.txt"));
        p2TextArea.setCaretPosition(0);
        loadingP2 = false;
    }

    private void saveFile(String path, FixedJTextArea area) {
        try {
            Files.writeString(Paths.get(path), area.getText(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {}
    }

    private static FixedJTextArea createTextArea(boolean editable) {
        FixedJTextArea area = new FixedJTextArea();
        area.setEditable(editable);
        area.setFont(org.example.I18n.font(Font.PLAIN, 13));
        area.setTabSize(4);
        area.setLineWrap(Main.WORD_WRAP);
        area.setWrapStyleWord(false);
        return area;
    }

    private void applyWordWrap() {
        boolean wrap = Main.WORD_WRAP;
        p1TextArea.setLineWrap(wrap);
        p2TextArea.setLineWrap(wrap);
        resultTextArea.setLineWrap(wrap);
        simpleResultTextArea.setLineWrap(wrap);
        p1TextArea.revalidate();
        p2TextArea.revalidate();
        resultTextArea.revalidate();
        simpleResultTextArea.revalidate();
    }

    static String readFileAutoEncoding(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) return "";
            return Setting.readUtf8(Files.readAllBytes(path));
        } catch (IOException e) {
            return org.example.I18n.t("battle.readError") + e.getMessage();
        }
    }

    private void refreshResultTabs() {
        resultTextArea.setText(readFileAutoEncoding("result.txt"));
        resultTextArea.setCaretPosition(0);
        simpleResultTextArea.setText(readFileAutoEncoding("simple_result.txt"));
        simpleResultTextArea.setCaretPosition(0);
    }
}
