package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MainGUI extends JFrame {

    // 设置控件
    private final JSpinner frameLimitSpinner;
    private final JSpinner threadSpinner;
    private final JCheckBox showHpCheck;
    private final JButton importButton;
    private final JButton battleButton;
    private final JProgressBar progressBar;
    private final JLabel statusLabel;
    private final JLabel timeLabel;
    private final DefaultTableModel tableModel;
    private final JTable statsTable;

    // 文本区域
    private final JTextArea p1TextArea;
    private final JTextArea p2TextArea;
    private final JTextArea resultTextArea;
    private final JTextArea simpleResultTextArea;

    private long battleStartTime;

    public MainGUI() {
        setTitle("激突对战工具");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1050, 720);
        setLocationRelativeTo(null);

        // 菜单栏
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("文件");
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> shutdown());
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // 创建各标签页组件
        JPanel battleTab = new JPanel(new BorderLayout(8, 8));
        battleTab.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // --- 设置面板 (NORTH) ---
        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        settingsPanel.setBorder(BorderFactory.createTitledBorder("设置"));

        settingsPanel.add(new JLabel("帧数上限:"));
        frameLimitSpinner = new JSpinner(new SpinnerNumberModel(65536, 0, Integer.MAX_VALUE, 1000));
        frameLimitSpinner.setPreferredSize(new Dimension(90, 22));
        settingsPanel.add(frameLimitSpinner);

        settingsPanel.add(new JLabel("线程数:"));
        int cores = Runtime.getRuntime().availableProcessors();
        threadSpinner = new JSpinner(new SpinnerNumberModel(cores, 1, 256, 1));
        threadSpinner.setPreferredSize(new Dimension(60, 22));
        settingsPanel.add(threadSpinner);

        showHpCheck = new JCheckBox("血量积分");
        settingsPanel.add(showHpCheck);

        importButton = new JButton("导入阵容");
        settingsPanel.add(importButton);

        battleButton = new JButton("开始对战");
        battleButton.setEnabled(false);
        settingsPanel.add(battleButton);

        battleTab.add(settingsPanel, BorderLayout.NORTH);

        // --- 统计表格 (CENTER) ---
        tableModel = new DefaultTableModel(
                new String[]{"1P阵容", "胜", "负", "平", "未定", "胜率", "积分"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 0 ? String.class : (columnIndex == 5 ? String.class : Integer.class);
            }
        };
        statsTable = new JTable(tableModel);
        statsTable.setFillsViewportHeight(true);
        statsTable.getTableHeader().setReorderingAllowed(false);
        statsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        statsTable.getColumnModel().getColumn(5).setPreferredWidth(60);
        JScrollPane tableScroll = new JScrollPane(statsTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("战绩统计"));
        battleTab.add(tableScroll, BorderLayout.CENTER);

        // --- 进度面板 (SOUTH) ---
        JPanel progressPanel = new JPanel(new BorderLayout(8, 4));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressPanel.add(progressBar, BorderLayout.NORTH);

        JPanel statusRow = new JPanel(new BorderLayout());
        statusLabel = new JLabel("就绪");
        timeLabel = new JLabel("");
        statusRow.add(statusLabel, BorderLayout.WEST);
        statusRow.add(timeLabel, BorderLayout.EAST);
        progressPanel.add(statusRow, BorderLayout.SOUTH);
        progressPanel.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        battleTab.add(progressPanel, BorderLayout.SOUTH);

        // --- 文本区域 ---
        p1TextArea = createTextArea(true);
        p2TextArea = createTextArea(true);
        resultTextArea = createTextArea(false);
        simpleResultTextArea = createTextArea(false);

        // --- 标签页 ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("对战控制", battleTab);
        tabbedPane.addTab("1P阵容", createEditorTab(p1TextArea, "1P.txt"));
        tabbedPane.addTab("2P阵容", createEditorTab(p2TextArea, "2P.txt"));
        tabbedPane.addTab("详细结果", createViewerTab(resultTextArea));
        tabbedPane.addTab("简要结果", createViewerTab(simpleResultTextArea));
        add(tabbedPane);

        // 监听值变化，自动保存配置
        frameLimitSpinner.addChangeListener(e -> applySettings());
        showHpCheck.addChangeListener(e -> applySettings());
        threadSpinner.addChangeListener(e -> applySettings());

        // 按钮事件
        importButton.addActionListener(e -> importForts());
        battleButton.addActionListener(e -> startBattle());

        // 初始化控件值
        frameLimitSpinner.setValue(Main.MAX_FRAME_LIMIT);
        threadSpinner.setValue(Main.MAX_THREADS);
        showHpCheck.setSelected(Main.SHOW_REMAIN_HP);

        // 加载编辑器内容
        loadFileToTextArea("1P.txt", p1TextArea);
        loadFileToTextArea("2P.txt", p2TextArea);
        refreshResultTabs();

        // 关闭时清理
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                shutdown();
            }
        });
    }

    private void applySettings() {
        Main.MAX_FRAME_LIMIT = (int) frameLimitSpinner.getValue();
        Main.MAX_THREADS = (int) threadSpinner.getValue();
        Main.SHOW_REMAIN_HP = showHpCheck.isSelected();
        Setting.saveConfig();

        if (Main.pool != null && !Main.pool.isShutdown()) {
            Main.pool.shutdown();
        }
        Main.pool = java.util.concurrent.Executors.newFixedThreadPool(Main.MAX_THREADS);
    }

    private void importForts() {
        applySettings();
        statusLabel.setText("正在导入阵容...");
        progressBar.setValue(0);
        timeLabel.setText("");
        setButtonsEnabled(false);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                Main.p1List = Setting.CompileForts("1P.txt");
                Main.p2List = Setting.CompileForts("2P.txt");
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    tableModel.setRowCount(0);
                    if (Main.p1List != null) {
                        for (CompiledFort f : Main.p1List) {
                            tableModel.addRow(new Object[]{f.name, 0, 0, 0, 0, "—", 0});
                        }
                    }
                    int total = (Main.p1List != null ? Main.p1List.size() : 0)
                              * (Main.p2List != null ? Main.p2List.size() : 0);
                    statusLabel.setText("导入完成: " + (Main.p1List != null ? Main.p1List.size() : 0)
                            + " × " + (Main.p2List != null ? Main.p2List.size() : 0)
                            + " = " + total + " 场对局");
                    battleButton.setEnabled(Main.p1List != null && !Main.p1List.isEmpty()
                            && Main.p2List != null && !Main.p2List.isEmpty());
                } catch (Exception e) {
                    statusLabel.setText("导入失败: " + e.getMessage());
                    e.printStackTrace();
                }
                setButtonsEnabled(true);
            }
        };
        worker.execute();
    }

    private void startBattle() {
        applySettings();
        setButtonsEnabled(false);
        progressBar.setValue(0);
        statusLabel.setText("对战中...");
        timeLabel.setText("");
        battleStartTime = System.nanoTime();

        SwingWorker<List<Main.FortStats>, Integer> worker = new SwingWorker<>() {
            @Override
            protected List<Main.FortStats> doInBackground() {
                return Main.runAllBattles(this::publish);
            }

            @Override
            protected void process(List<Integer> chunks) {
                int latest = chunks.get(chunks.size() - 1);
                int total = (Main.p1List != null ? Main.p1List.size() : 1)
                          * (Main.p2List != null ? Main.p2List.size() : 1);
                if (total > 0) {
                    int pct = latest * 100 / total;
                    progressBar.setValue(pct);
                    progressBar.setString(latest + " / " + total);
                }
                long elapsed = (System.nanoTime() - battleStartTime) / 1_000_000L;
                timeLabel.setText(String.format("已用时: %.1fs", elapsed / 1000.0));
            }

            @Override
            protected void done() {
                try {
                    List<Main.FortStats> stats = get();
                    tableModel.setRowCount(0);
                    if (stats != null) {
                        for (Main.FortStats s : stats) {
                            tableModel.addRow(new Object[]{
                                    s.name, s.win, s.lose, s.draw, s.unknown,
                                    String.format("%.2f%%", s.winRate()),
                                    s.score
                            });
                        }
                    }
                    refreshResultTabs();

                    long elapsed = (System.nanoTime() - battleStartTime) / 1_000_000L;
                    progressBar.setValue(100);
                    progressBar.setString("完成");
                    statusLabel.setText("对战完成！");
                    timeLabel.setText(String.format("总用时: %.1fs", elapsed / 1000.0));
                } catch (Exception e) {
                    statusLabel.setText("对战出错: " + e.getMessage());
                    e.printStackTrace();
                }
                setButtonsEnabled(true);
            }
        };
        worker.execute();
    }

    private void setButtonsEnabled(boolean enabled) {
        importButton.setEnabled(enabled);
        battleButton.setEnabled(enabled && Main.p1List != null && !Main.p1List.isEmpty()
                && Main.p2List != null && !Main.p2List.isEmpty());
    }

    // ---- 编辑器标签页 ----

    private JPanel createEditorTab(JTextArea textArea, String filePath) {
        JPanel panel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton saveBtn = new JButton("保存");
        saveBtn.addActionListener(e -> saveTextAreaToFile(filePath, textArea));
        btnPanel.add(saveBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ---- 查看器标签页 ----

    private JPanel createViewerTab(JTextArea textArea) {
        JPanel panel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(textArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refreshBtn = new JButton("刷新");
        refreshBtn.addActionListener(e -> refreshResultTabs());
        btnPanel.add(refreshBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    // ---- 文件 I/O ----

    private static JTextArea createTextArea(boolean editable) {
        JTextArea area = new JTextArea();
        area.setEditable(editable);
        area.setFont(new Font("Consolas", Font.PLAIN, 13));
        area.setTabSize(4);
        return area;
    }

    private static void loadFileToTextArea(String filePath, JTextArea area) {
        area.setText(readFileAutoEncoding(filePath));
    }

    static String readFileAutoEncoding(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path)) {
                return "";
            }
            byte[] bytes = Files.readAllBytes(path);
            return Setting.decodeBytes(bytes);
        } catch (IOException e) {
            return "读取文件失败: " + e.getMessage();
        }
    }

    private static void saveTextAreaToFile(String filePath, JTextArea area) {
        try {
            Files.writeString(Paths.get(filePath), area.getText(),
                    StandardCharsets.UTF_8);
            JOptionPane.showMessageDialog(null, filePath + " 保存成功", "保存", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "保存失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshResultTabs() {
        loadFileToTextArea("result.txt", resultTextArea);
        loadFileToTextArea("simple_result.txt", simpleResultTextArea);
    }

    private void shutdown() {
        if (Main.pool != null && !Main.pool.isShutdown()) {
            Main.pool.shutdown();
        }
        dispose();
        System.exit(0);
    }
}
