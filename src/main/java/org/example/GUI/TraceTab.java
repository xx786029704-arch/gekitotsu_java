package org.example.GUI;

import org.example.*;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** 轨迹预测标签页：轨迹/要塞壁双标签页管理 + 编辑对话框 + 色板 + 拖拽排序。 */
public class TraceTab extends JPanel {

    private static final Color[] TRACE_PALETTE = {
        new Color(0xE0, 0x3E, 0x3E),
        new Color(0x3E, 0x8E, 0xE0),
        new Color(0xE0, 0xC8, 0x3E),
        new Color(0xE0, 0x7B, 0x3E),
        new Color(0x3E, 0xE0, 0x6E),
        new Color(0x8E, 0x3E, 0xE0),
        new Color(0x73, 0x41, 0x26),
        new Color(0x3E, 0xD0, 0xD0),
        new Color(0x00, 0x00, 0x00),
    };

    private static final int ITEM_HEIGHT = 37;

    private final JFrame parentFrame;
    private final TraceCanvas traceCanvas;

    // 轨迹数据
    private final ArrayList<Trace> traces = new ArrayList<>();
    private final DropListPanel traceListInnerPanel;
    private final Set<Trace> selectedTraces = new HashSet<>();
    private Trace lastClickedTrace;
    private DragState<Trace> traceDrag;

    // 要塞壁数据
    private final ArrayList<TraceWall> walls = new ArrayList<>();
    private final DropListPanel wallListInnerPanel;
    private final Set<TraceWall> selectedWalls = new HashSet<>();
    private TraceWall lastClickedWall;
    private DragState<TraceWall> wallDrag;

    // 变量数据
    private final ArrayList<Variable> variables = new ArrayList<>();
    private final DropListPanel variableListInnerPanel;
    private final Set<Variable> selectedVariables = new HashSet<>();
    private Variable lastClickedVariable;
    private DragState<Variable> variableDrag;

    private final JTabbedPane listTabs;

    public TraceTab(JFrame parentFrame) {
        super(new BorderLayout(8, 8));
        this.parentFrame = parentFrame;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        traceCanvas = new TraceCanvas(traces, walls, variables);
        traceCanvas.setBackground(Main.DARK_MODE ? new Color(0x2D, 0x2D, 0x2D) : Color.WHITE);
        traceCanvas.setBorder(BorderFactory.createTitledBorder("预览"));
        traceCanvas.setMinimumSize(new Dimension(200, 200));

        JPanel traceSettingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        traceSettingsPanel.setBorder(BorderFactory.createTitledBorder("轨迹设置"));

        JCheckBox showCenterCheck = new JCheckBox("显示中心", true);
        showCenterCheck.addChangeListener(e -> {
            traceCanvas.showCenter = showCenterCheck.isSelected();
            traceCanvas.repaint();
        });
        traceSettingsPanel.add(showCenterCheck);

        JCheckBox showLandingCheck = new JCheckBox("显示落地判定");
        showLandingCheck.addChangeListener(e -> {
            traceCanvas.showLandingDetection = showLandingCheck.isSelected();
            traceCanvas.repaint();
        });
        traceSettingsPanel.add(showLandingCheck);

        JCheckBox showWallCheck = new JCheckBox("显示撞墙判定");
        showWallCheck.addChangeListener(e -> {
            traceCanvas.showWallDetection = showWallCheck.isSelected();
            traceCanvas.repaint();
        });
        traceSettingsPanel.add(showWallCheck);

        JCheckBox showOutlineCheck = new JCheckBox("显示兵玉轮廓");
        showOutlineCheck.addChangeListener(e -> {
            traceCanvas.showUnitOutline = showOutlineCheck.isSelected();
            traceCanvas.repaint();
        });
        traceSettingsPanel.add(showOutlineCheck);

        JCheckBox showFortOutlineCheck = new JCheckBox("显示阵型边界");
        showFortOutlineCheck.addChangeListener(e -> {
            traceCanvas.showFortOutline = showFortOutlineCheck.isSelected();
            traceCanvas.repaint();
        });
        traceSettingsPanel.add(showFortOutlineCheck);

        add(traceSettingsPanel, BorderLayout.NORTH);

        // 轨迹列表
        traceListInnerPanel = new DropListPanel();
        traceListInnerPanel.setLayout(new BoxLayout(traceListInnerPanel, BoxLayout.Y_AXIS));
        traceListInnerPanel.setBackground(listBg());
        JScrollPane traceScrollPane = new JScrollPane(traceListInnerPanel);
        traceScrollPane.setPreferredSize(new Dimension(48, 0));

        // 要塞壁列表
        wallListInnerPanel = new DropListPanel();
        wallListInnerPanel.setLayout(new BoxLayout(wallListInnerPanel, BoxLayout.Y_AXIS));
        wallListInnerPanel.setBackground(listBg());
        JScrollPane wallScrollPane = new JScrollPane(wallListInnerPanel);
        wallScrollPane.setPreferredSize(new Dimension(48, 0));

        // 变量列表
        variableListInnerPanel = new DropListPanel();
        variableListInnerPanel.setLayout(new BoxLayout(variableListInnerPanel, BoxLayout.Y_AXIS));
        variableListInnerPanel.setBackground(listBg());
        JScrollPane variableScrollPane = new JScrollPane(variableListInnerPanel);
        variableScrollPane.setPreferredSize(new Dimension(48, 0));

        traceDrag = new DragState<>(traces, traceListInnerPanel, this::refreshTraceList);
        wallDrag = new DragState<>(walls, wallListInnerPanel, this::refreshWallList);
        variableDrag = new DragState<>(variables, variableListInnerPanel, this::refreshVariableList);

        listTabs = new JTabbedPane();
        listTabs.addTab("轨迹", traceScrollPane);
        listTabs.addTab("要塞壁", wallScrollPane);
        listTabs.addTab("变量", variableScrollPane);
        setupKeyBindings();

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listTabs, traceCanvas);
        splitPane.setResizeWeight(0.167);
        add(splitPane, BorderLayout.CENTER);

        refreshTraceList();
        refreshWallList();

        java.awt.event.MouseAdapter clearSelectionAdapter = new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                deselectAll();
            }
        };
        traceCanvas.addMouseListener(clearSelectionAdapter);
        traceSettingsPanel.addMouseListener(clearSelectionAdapter);
        traceListInnerPanel.addMouseListener(clearSelectionAdapter);
        traceScrollPane.getViewport().addMouseListener(clearSelectionAdapter);
        wallListInnerPanel.addMouseListener(clearSelectionAdapter);
        wallScrollPane.getViewport().addMouseListener(clearSelectionAdapter);
        variableListInnerPanel.addMouseListener(clearSelectionAdapter);
        variableScrollPane.getViewport().addMouseListener(clearSelectionAdapter);
        listTabs.addMouseListener(clearSelectionAdapter);
        addMouseListener(clearSelectionAdapter);
        Color c = Main.DARK_MODE ? new Color(0xDD, 0xDD, 0xDD) : new Color(0x30, 0x30, 0x30);
        walls.add(new TraceWall("138", "132", "要塞核心", true, c, true));
        refreshWallList();
        refreshVariableList();
    }

    // --- 帮助方法 ---

    private Color listBg() {
        return Main.DARK_MODE ? new Color(0x3A, 0x3A, 0x3A) : new Color(0xFA, 0xFA, 0xFA);
    }

    private boolean isWallTabActive() {
        return listTabs.getSelectedIndex() == 1;
    }

    private boolean isVariableTabActive() {
        return listTabs.getSelectedIndex() == 2;
    }

    void updateDarkMode() {
        traceListInnerPanel.setBackground(listBg());
        wallListInnerPanel.setBackground(listBg());
        variableListInnerPanel.setBackground(listBg());
        traceCanvas.updateDarkMode();
        refreshTraceList();
        refreshWallList();
        refreshVariableList();
    }

    private Color pickUnusedTraceColor() {
        Set<Color> used = new HashSet<>();
        for (Trace t : traces) used.add(t.color);
        return pickUnusedColor(used);
    }

    private Color pickUnusedWallColor() {
        Set<Color> used = new HashSet<>();
        for (TraceWall w : walls) used.add(w.color);
        return pickUnusedColor(used);
    }

    private Color pickUnusedColor(Set<Color> used) {
        List<Color> unused = new ArrayList<>();
        for (Color c : TRACE_PALETTE) {
            Color effective = c;
            if (Main.DARK_MODE && c.equals(Color.BLACK)) effective = Color.WHITE;
            if (!used.contains(effective)) unused.add(effective);
        }
        if (unused.isEmpty()) {
            int i = new Random().nextInt(TRACE_PALETTE.length);
            if (Main.DARK_MODE && TRACE_PALETTE[i].equals(Color.BLACK)) return Color.WHITE;
            return TRACE_PALETTE[i];
        }
        return unused.get(new Random().nextInt(unused.size()));
    }

    // --- 轨迹列表刷新 ---

    private void refreshTraceList() {
        traceListInnerPanel.removeAll();
        traceDrag.reset();
        traceListInnerPanel.dropLineY = -1;

        for (int i = 0; i < traces.size(); i++) {
            final int idx = i;
            Trace t = traces.get(i);
            TraceItemPanel item = new TraceItemPanel(t,
                    () -> showTraceDialog(traces.get(idx)),
                    () -> traceCanvas.repaint(),
                    () -> traceDrag.start(idx),
                    () -> traceDrag.update(),
                    () -> traceDrag.end(),
                    e -> handleSelect(t, e));
            if (selectedTraces.contains(t)) item.setSelected(true);
            traceListInnerPanel.add(item);
        }

        JButton addButton = new JButton("＋ 添加轨迹");
        addButton.setHorizontalAlignment(SwingConstants.LEFT);
        addButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addButton.getPreferredSize().height));
        addButton.addActionListener(e -> showTraceDialog(null));

        JPanel btnWrapper = new JPanel(new BorderLayout());
        btnWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, addButton.getPreferredSize().height));
        btnWrapper.add(addButton, BorderLayout.CENTER);
        traceListInnerPanel.add(btnWrapper);

        JButton importButton = new JButton("＋ 从代码中导入");
        importButton.setHorizontalAlignment(SwingConstants.LEFT);
        importButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, importButton.getPreferredSize().height));
        importButton.addActionListener(e -> showTraceImportDialog());
        JPanel importWrapper = new JPanel(new BorderLayout());
        importWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, importButton.getPreferredSize().height));
        importWrapper.add(importButton, BorderLayout.CENTER);
        traceListInnerPanel.add(importWrapper);

        traceListInnerPanel.revalidate();
        traceListInnerPanel.repaint();
        traceCanvas.repaint();
    }

    // --- 要塞壁列表刷新 ---

    private void refreshWallList() {
        wallListInnerPanel.removeAll();
        wallDrag.reset();
        wallListInnerPanel.dropLineY = -1;

        for (int i = 0; i < walls.size(); i++) {
            final int idx = i;
            TraceWall w = walls.get(i);
            TraceItemPanel item = new TraceItemPanel(w,
                    () -> showWallDialog(walls.get(idx)),
                    () -> traceCanvas.repaint(),
                    () -> wallDrag.start(idx),
                    () -> wallDrag.update(),
                    () -> wallDrag.end(),
                    e -> handleWallSelect(w, e));
            if (selectedWalls.contains(w)) item.setSelected(true);
            wallListInnerPanel.add(item);
        }

        JButton addButton = new JButton("＋ 添加要塞壁");
        addButton.setHorizontalAlignment(SwingConstants.LEFT);
        addButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addButton.getPreferredSize().height));
        addButton.addActionListener(e -> showWallDialog(null));

        JPanel btnWrapper = new JPanel(new BorderLayout());
        btnWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, addButton.getPreferredSize().height));
        btnWrapper.add(addButton, BorderLayout.CENTER);
        wallListInnerPanel.add(btnWrapper);

        JButton importButton = new JButton("＋ 从代码中导入");
        importButton.setHorizontalAlignment(SwingConstants.LEFT);
        importButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, importButton.getPreferredSize().height));
        importButton.addActionListener(e -> showWallImportDialog());
        JPanel importWrapper = new JPanel(new BorderLayout());
        importWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, importButton.getPreferredSize().height));
        importWrapper.add(importButton, BorderLayout.CENTER);
        wallListInnerPanel.add(importWrapper);

        wallListInnerPanel.revalidate();
        wallListInnerPanel.repaint();
        traceCanvas.repaint();
    }

    // --- 变量列表刷新 ---

    private void refreshVariableList() {
        variableListInnerPanel.removeAll();
        variableDrag.reset();
        variableListInnerPanel.dropLineY = -1;

        for (int i = 0; i < variables.size(); i++) {
            final int idx = i;
            Variable v = variables.get(i);
            TraceItemPanel item = new TraceItemPanel(v,
                    () -> showVariableDialog(variables.get(idx)),
                    () -> traceCanvas.repaint(),
                    () -> {},
                    () -> {},
                    () -> {},
                    e -> handleVariableSelect(v, e));
            if (selectedVariables.contains(v)) item.setSelected(true);
            variableListInnerPanel.add(item);
        }

        JButton addButton = new JButton("＋ 添加变量");
        addButton.setHorizontalAlignment(SwingConstants.LEFT);
        addButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, addButton.getPreferredSize().height));
        addButton.addActionListener(e -> showVariableDialog(null));

        JPanel btnWrapper = new JPanel(new BorderLayout());
        btnWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, addButton.getPreferredSize().height));
        btnWrapper.add(addButton, BorderLayout.CENTER);
        variableListInnerPanel.add(btnWrapper);
        variableListInnerPanel.revalidate();
        variableListInnerPanel.repaint();
        traceCanvas.repaint();
    }

    // --- 变量编辑对话框 ---

    private void showVariableDialog(Variable existing) {
        boolean isEdit = existing != null;
        JDialog dialog = new JDialog(parentFrame, isEdit ? "编辑变量" : "添加变量", true);
        dialog.setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // 新建时默认使用首个未被占用的字母
        String defaultName;
        if (isEdit) {
            defaultName = existing.name;
        } else {
            Set<String> used = new HashSet<>();
            for (Variable v : variables) used.add(v.name);
            String first = Variable.firstUnusedLetter(used);
            defaultName = first != null ? first : "";
        }
        JTextField nameField = new JTextField(defaultName);
        JTextField subField  = new JTextField(existing != null ? String.valueOf(existing.sub) : "15");
        JTextField infField  = new JTextField(existing != null ? String.valueOf(existing.inf) : "0");
        JTextField valField  = new JTextField(existing != null ? String.valueOf(existing.val) : "0");

        form.add(new JLabel("变量名称 (a-z):"));
        form.add(nameField);
        form.add(new JLabel("最大值 (sub):"));
        form.add(subField);
        form.add(new JLabel("最小值 (inf):"));
        form.add(infField);
        form.add(new JLabel("当前值 (val):"));
        form.add(valField);

        dialog.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton saveBtn = new JButton("保存");
        JButton cancelBtn = new JButton("取消");

        if (isEdit) {
            JButton deleteBtn = new JButton("删除");
            deleteBtn.addActionListener(e -> {
                selectedVariables.remove(existing);
                variables.remove(existing);
                refreshVariableList();
                dialog.dispose();
            });
            btnPanel.add(deleteBtn);
        }

        saveBtn.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                if (name.length() != 1 || name.charAt(0) < 'a' || name.charAt(0) > 'z') return;
                int sub = Integer.parseInt(subField.getText().trim());
                int inf = Integer.parseInt(infField.getText().trim());
                int val = Math.clamp(Integer.parseInt(valField.getText().trim()), inf, sub);

                // 重名校验：编辑时排除自身，新建时检查所有
                boolean duplicate = false;
                for (Variable v : variables) {
                    if (v != existing && v.name.equals(name)) {
                        duplicate = true;
                        break;
                    }
                }
                if (duplicate) {
                    JOptionPane.showMessageDialog(dialog, "变量已存在", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Variable v = new Variable(name, sub, inf, val);

                if (isEdit) {
                    int idx = variables.indexOf(existing);
                    variables.set(idx, v);
                    if (selectedVariables.remove(existing)) selectedVariables.add(v);
                } else {
                    variables.add(v);
                }
                refreshVariableList();
                dialog.dispose();
            } catch (NumberFormatException ignored) {}
        });

        cancelBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    }

    // --- 变量选择 ---

    private void handleVariableSelect(Variable v, MouseEvent e) {
        boolean ctrl = e.isControlDown();
        boolean shift = e.isShiftDown();

        if (shift && lastClickedVariable != null) {
            int from = variables.indexOf(lastClickedVariable);
            int to = variables.indexOf(v);
            if (from >= 0 && to >= 0) {
                if (!ctrl) selectedVariables.clear();
                int lo = Math.min(from, to);
                int hi = Math.max(from, to);
                for (int i = lo; i <= hi; i++) selectedVariables.add(variables.get(i));
            }
        } else if (ctrl) {
            if (selectedVariables.contains(v)) selectedVariables.remove(v);
            else selectedVariables.add(v);
            lastClickedVariable = v;
        } else {
            selectedVariables.clear();
            selectedVariables.add(v);
            lastClickedVariable = v;
        }
        for (int i = 0; i < variableListInnerPanel.getComponentCount(); i++) {
            Component c = variableListInnerPanel.getComponent(i);
            if (c instanceof TraceItemPanel item)
                item.setSelected(selectedVariables.contains(item.item));
        }
    }

    // --- 轨迹编辑对话框 ---

    private void showTraceDialog(Trace existing) {
        boolean isEdit = existing != null;
        JDialog dialog = new JDialog(parentFrame, isEdit ? "编辑轨迹" : "添加轨迹", true);
        dialog.setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextField xField  = new JTextField(existing != null ? existing.x : "");
        JTextField yField  = new JTextField(existing != null ? existing.y : "");
        JTextField wxField = new JTextField(existing != null ? existing.wall_x : "");
        JTextField sp0Field = new JTextField(existing != null ? existing.speed0 : "0");
        JTextField sp1Field = new JTextField(existing != null ? existing.speed1 : "0");
        JTextField timesField = new JTextField(existing != null ? existing.times : "1");
        JTextField nameField = new JTextField(existing != null ? existing.name : "轨迹");
        JCheckBox nearCheck = new JCheckBox("", existing != null && existing.isNear);
        JCheckBox ballFirstCheck = new JCheckBox("", existing != null && existing.isBallFirst);

        form.add(new JLabel("轨迹名称:"));
        form.add(nameField);
        form.add(new JLabel("突击壁x:"));
        form.add(wxField);
        form.add(new JLabel("兵玉x:"));
        form.add(xField);
        form.add(new JLabel("兵玉y:"));
        form.add(yField);
        form.add(new JLabel("1P加速度等级:"));
        form.add(sp0Field);
        form.add(new JLabel("2P加速度等级:"));
        form.add(sp1Field);
        form.add(new JLabel("突击段数:"));
        form.add(timesField);
        form.add(new JLabel("是否为近突击壁:"));
        form.add(nearCheck);
        form.add(new JLabel("兵玉代码在前:"));
        form.add(ballFirstCheck);

        Color[] selectedColor = { existing != null ? existing.color : pickUnusedTraceColor() };
        form.add(new JLabel("颜色:"));
        form.add(createColorSwatch(dialog, selectedColor));

        dialog.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton saveBtn = new JButton("保存");
        JButton cancelBtn = new JButton("取消");

        if (isEdit) {
            JButton deleteBtn = new JButton("删除");
            deleteBtn.addActionListener(e -> {
                selectedTraces.remove(existing);
                traces.remove(existing);
                refreshTraceList();
                dialog.dispose();
            });
            btnPanel.add(deleteBtn);
        }

        saveBtn.addActionListener(e -> {
            String x = xField.getText().trim();
            String y = yField.getText().trim();
            String wx = wxField.getText().trim();
            String sp0 = sp0Field.getText().trim();
            String sp1 = sp1Field.getText().trim();
            String times = timesField.getText().trim();
            String name = nameField.getText();

            Trace t = new Trace(x, y, wx, sp0, sp1, times,
                    nearCheck.isSelected(), ballFirstCheck.isSelected(), selectedColor[0], name);

            if (isEdit) {
                int idx = traces.indexOf(existing);
                traces.set(idx, t);
                if (selectedTraces.remove(existing)) selectedTraces.add(t);
            } else {
                traces.add(t);
            }
            refreshTraceList();
            dialog.dispose();
        });

        cancelBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    }

    // --- 要塞壁编辑对话框 ---

    private void showWallDialog(TraceWall existing) {
        boolean isEdit = existing != null;
        JDialog dialog = new JDialog(parentFrame, isEdit ? "编辑要塞壁" : "添加要塞壁", true);
        dialog.setLayout(new BorderLayout(8, 8));

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 6));
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextField nameField = new JTextField(existing != null ? existing.name : "要塞壁");
        JTextField xField = new JTextField(existing != null ? existing.x : "");
        JTextField yField = new JTextField(existing != null ? existing.y : "");
        JCheckBox coreCheck = new JCheckBox("", existing != null && existing.isCore);

        form.add(new JLabel("名称:"));
        form.add(nameField);
        form.add(new JLabel("x:"));
        form.add(xField);
        form.add(new JLabel("y:"));
        form.add(yField);
        form.add(new JLabel("核心:"));
        form.add(coreCheck);

        Color[] selectedColor = { existing != null ? existing.color : pickUnusedWallColor() };
        form.add(new JLabel("颜色:"));
        form.add(createColorSwatch(dialog, selectedColor));

        dialog.add(form, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton saveBtn = new JButton("保存");
        JButton cancelBtn = new JButton("取消");

        if (isEdit) {
            JButton deleteBtn = new JButton("删除");
            deleteBtn.addActionListener(e -> {
                selectedWalls.remove(existing);
                walls.remove(existing);
                refreshWallList();
                dialog.dispose();
            });
            btnPanel.add(deleteBtn);
        }

        saveBtn.addActionListener(e -> {
            String name = nameField.getText();
            boolean isCore = coreCheck.isSelected();
            String x = xField.getText().trim();
            String y = yField.getText().trim();
            TraceWall w = new TraceWall(x, y, name, true, selectedColor[0], isCore);

                if (isEdit) {
                    int idx = walls.indexOf(existing);
                    walls.set(idx, w);
                    if (selectedWalls.remove(existing)) selectedWalls.add(w);
                } else {
                    walls.add(w);
                }
                refreshWallList();
                dialog.dispose();
        });

        cancelBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(cancelBtn);
        btnPanel.add(saveBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    }

    // --- 从代码批量导入 ---

    /** 从阵容代码导入要塞壁：解析 → 稳定化 → 提取所有 isWallLike 单位。 */
    private void showWallImportDialog() {
        JDialog dialog = new JDialog(parentFrame, "从代码导入要塞壁", true);
        dialog.setLayout(new BorderLayout(8, 8));

        JTextArea codeArea = new JTextArea(8, 40);
        codeArea.setFont(new Font("黑体", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(codeArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("阵容代码"));
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton importBtn = new JButton("导入");
        JButton cancelBtn = new JButton("取消");

        importBtn.addActionListener(e -> {
            String code = codeArea.getText().trim();
            if (code.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "请输入阵容代码", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Formation formation = Formation.decode(code);
                List<Unit> stabilized = FormationStabilizer.stabilize(formation.units);

                walls.removeIf(w -> w.isCore);

                for (Unit u : stabilized) {
                    if (!u.isWallLike()) continue;
                    String x = String.valueOf(u.x);
                    String y = String.valueOf(u.y);
                    String name = Unit.infos[u.id].name();
                    boolean isCore = u.isCore();
                    walls.add(new TraceWall(x, y, name, true, pickUnusedWallColor(), isCore));
                }
                refreshWallList();
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "代码解析失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(cancelBtn);
        btnPanel.add(importBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    }

    /** 从阵容代码导入轨迹：解析 → 稳定化 → 检测突击组 → 每个单位生成一条 Trace。 */
    private void showTraceImportDialog() {
        JDialog dialog = new JDialog(parentFrame, "从代码导入轨迹", true);
        dialog.setLayout(new BorderLayout(8, 8));

        JPanel topPanel = new JPanel(new BorderLayout(8, 8));
        topPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JTextArea codeArea = new JTextArea(8, 40);
        codeArea.setFont(new Font("黑体", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(codeArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("阵容代码"));

        JPanel speedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        speedPanel.add(new JLabel("对方加速度等级:"));
        JTextField speedField = new JTextField("0", 5);
        speedPanel.add(speedField);

        topPanel.add(scrollPane, BorderLayout.CENTER);
        topPanel.add(speedPanel, BorderLayout.SOUTH);
        dialog.add(topPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton importBtn = new JButton("导入");
        JButton cancelBtn = new JButton("取消");

        importBtn.addActionListener(e -> {
            String code = codeArea.getText().trim();
            String speed1Str = speedField.getText().trim();
            if (code.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "请输入阵容代码", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                Formation formation = Formation.decode(code);
                int speed0 = formation.getAccelLevel();
                List<Unit> stabilized = FormationStabilizer.stabilize(formation.units);
                List<AssaultGroup> groups = AssaultDetector.detect(stabilized);

                for (AssaultGroup g : groups) {
                    for (Unit u : g.unitsBefore) {
                        traces.add(new Trace(
                                String.valueOf(u.x),
                                String.valueOf(u.y),
                                String.valueOf(g.wallX),
                                String.valueOf(speed0),
                                speed1Str,
                                "1",
                                !g.isFar,
                                true,
                                pickUnusedTraceColor(),
                                Unit.infos[u.id].name()
                        ));
                    }
                    for (Unit u : g.unitsAfter) {
                        traces.add(new Trace(
                                String.valueOf(u.x),
                                String.valueOf(u.y),
                                String.valueOf(g.wallX),
                                String.valueOf(speed0),
                                speed1Str,
                                "1",
                                !g.isFar,
                                false,
                                pickUnusedTraceColor(),
                                Unit.infos[u.id].name()
                        ));
                    }
                }

                if (groups.isEmpty()) {
                    JOptionPane.showMessageDialog(dialog, "未检测到任何突击组", "提示", JOptionPane.INFORMATION_MESSAGE);
                }

                refreshTraceList();
                dialog.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "代码解析失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());
        btnPanel.add(cancelBtn);
        btnPanel.add(importBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setVisible(true);
    }

    /** 创建颜色选择色块组件，供两个对话框复用。 */
    private JPanel createColorSwatch(JDialog dialog, Color[] selectedColor) {
        JPanel colorSwatch = new JPanel() {
            {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(selectedColor[0]);
                g2.fillRect(2, 2, getWidth() - 4, getHeight() - 4);
                g2.setColor(Main.DARK_MODE ? Color.WHITE : Color.DARK_GRAY);
                g2.drawRect(2, 2, getWidth() - 4, getHeight() - 4);
                g2.dispose();
            }
            @Override
            public Dimension getPreferredSize() { return new Dimension(50, 26); }
            @Override
            public Dimension getMinimumSize() { return new Dimension(50, 26); }
        };
        colorSwatch.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                Color result = ColorPicker.showDialog(dialog, selectedColor[0], Main.DARK_MODE, false);
                if (result != null) {
                    selectedColor[0] = result;
                    colorSwatch.repaint();
                }
            }
        });
        return colorSwatch;
    }

    // --- 轨迹选择 ---

    private void handleSelect(Trace t, MouseEvent e) {
        boolean ctrl = e.isControlDown();
        boolean shift = e.isShiftDown();

        if (shift && lastClickedTrace != null) {
            int from = traces.indexOf(lastClickedTrace);
            int to = traces.indexOf(t);
            if (from >= 0 && to >= 0) {
                if (!ctrl) selectedTraces.clear();
                int lo = Math.min(from, to);
                int hi = Math.max(from, to);
                for (int i = lo; i <= hi; i++) selectedTraces.add(traces.get(i));
            }
        } else if (ctrl) {
            if (selectedTraces.contains(t)) selectedTraces.remove(t);
            else selectedTraces.add(t);
            lastClickedTrace = t;
        } else {
            selectedTraces.clear();
            selectedTraces.add(t);
            lastClickedTrace = t;
        }
        for (int i = 0; i < traceListInnerPanel.getComponentCount(); i++) {
            Component c = traceListInnerPanel.getComponent(i);
            if (c instanceof TraceItemPanel item)
                item.setSelected(selectedTraces.contains(item.item));
        }
    }

    // --- 要塞壁选择 ---

    private void handleWallSelect(TraceWall w, MouseEvent e) {
        boolean ctrl = e.isControlDown();
        boolean shift = e.isShiftDown();

        if (shift && lastClickedWall != null) {
            int from = walls.indexOf(lastClickedWall);
            int to = walls.indexOf(w);
            if (from >= 0 && to >= 0) {
                if (!ctrl) selectedWalls.clear();
                int lo = Math.min(from, to);
                int hi = Math.max(from, to);
                for (int i = lo; i <= hi; i++) selectedWalls.add(walls.get(i));
            }
        } else if (ctrl) {
            if (selectedWalls.contains(w)) selectedWalls.remove(w);
            else selectedWalls.add(w);
            lastClickedWall = w;
        } else {
            selectedWalls.clear();
            selectedWalls.add(w);
            lastClickedWall = w;
        }
        for (int i = 0; i < wallListInnerPanel.getComponentCount(); i++) {
            Component c = wallListInnerPanel.getComponent(i);
            if (c instanceof TraceItemPanel item)
                item.setSelected(selectedWalls.contains(item.item));
        }
    }

    // --- 键盘快捷键 ---

    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
        am.put("delete", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { deleteSelected(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_DOWN_MASK), "copy");
        am.put("copy", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { copySelected(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK), "paste");
        am.put("paste", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { pasteSelected(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.CTRL_DOWN_MASK), "cut");
        am.put("cut", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { cutSelected(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.CTRL_DOWN_MASK), "selectAll");
        am.put("selectAll", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) { selectAll(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK), "deselectAll");
        am.put("deselectAll", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent e) {
                deselectAll();
            }
        });
    }

    private JPanel getActiveListPanel() {
        if (isWallTabActive()) return wallListInnerPanel;
        if (isVariableTabActive()) return variableListInnerPanel;
        return traceListInnerPanel;
    }

    // --- 剪贴板操作 ---

    private void copySelected() {
        if (isVariableTabActive()) {
            if (selectedVariables.isEmpty()) return;
            List<Variable> ordered = new ArrayList<>();
            for (Variable v : variables)
                if (selectedVariables.contains(v)) ordered.add(v);
            String text = Variable.serializeMultiple(ordered);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
        } else if (isWallTabActive()) {
            if (selectedWalls.isEmpty()) return;
            List<TraceWall> ordered = new ArrayList<>();
            for (TraceWall w : walls)
                if (selectedWalls.contains(w)) ordered.add(w);
            String text = TraceWall.serializeMultiple(ordered);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
        } else {
            if (selectedTraces.isEmpty()) return;
            List<Trace> ordered = new ArrayList<>();
            for (Trace t : traces)
                if (selectedTraces.contains(t)) ordered.add(t);
            String text = Trace.serializeMultiple(ordered);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
        }
    }

    private void pasteSelected() {
        try {
            String text = (String) java.awt.Toolkit.getDefaultToolkit()
                    .getSystemClipboard().getData(DataFlavor.stringFlavor);
            if (text == null || text.isEmpty()) return;

            if (isVariableTabActive()) {
                List<Variable> parsed = Variable.deserializeMultiple(text);
                if (parsed.isEmpty()) return;
                selectedVariables.clear();
                for (Variable v : parsed) {
                    Variable conflict = null;
                    for (Variable exist : variables) {
                        if (exist.name.equals(v.name)) { conflict = exist; break; }
                    }
                    if (conflict == null) {
                        selectedVariables.add(v);
                        variables.add(v);
                    } else {
                        int choice = JOptionPane.showOptionDialog(
                                this,
                                "变量 '" + v.name + "' 已存在，如何处理？",
                                "变量冲突",
                                JOptionPane.YES_NO_CANCEL_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                new String[]{"替换", "取消", "重命名"},
                                "替换");
                        if (choice == 0) { // 替换
                            int idx = variables.indexOf(conflict);
                            variables.set(idx, v);
                            selectedVariables.remove(conflict);
                            selectedVariables.add(v);
                        } else if (choice == 2) { // 重命名
                            Set<String> used = new HashSet<>();
                            for (Variable x : variables) used.add(x.name);
                            String newName = Variable.firstUnusedLetter(used);
                            if (newName != null) {
                                v.name = newName;
                                selectedVariables.add(v);
                                variables.add(v);
                            }
                        }
                        // choice == 1 (取消) 或对话框关闭: 跳过该变量
                    }
                }
                refreshVariableList();
            } else if (isWallTabActive()) {
                List<TraceWall> parsed = TraceWall.deserializeMultiple(text);
                if (parsed.isEmpty()) return;
                selectedWalls.clear();
                for (TraceWall w : parsed) {
                    selectedWalls.add(w);
                    walls.add(w);
                }
                refreshWallList();
            } else {
                List<Trace> parsed = Trace.deserializeMultiple(text);
                if (parsed.isEmpty()) return;
                selectedTraces.clear();
                for (Trace t : parsed) {
                    selectedTraces.add(t);
                    traces.add(t);
                }
                refreshTraceList();
            }
            getActiveListPanel().requestFocusInWindow();
        } catch (Exception ignored) {}
    }

    private void cutSelected() {
        if (isVariableTabActive()) {
            if (selectedVariables.isEmpty()) return;
            List<Variable> ordered = new ArrayList<>();
            for (Variable v : variables)
                if (selectedVariables.contains(v)) ordered.add(v);
            String text = Variable.serializeMultiple(ordered);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
            variables.removeAll(selectedVariables);
            selectedVariables.clear();
            refreshVariableList();
        } else if (isWallTabActive()) {
            if (selectedWalls.isEmpty()) return;
            List<TraceWall> ordered = new ArrayList<>();
            for (TraceWall w : walls)
                if (selectedWalls.contains(w)) ordered.add(w);
            String text = TraceWall.serializeMultiple(ordered);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
            walls.removeAll(selectedWalls);
            selectedWalls.clear();
            refreshWallList();
        } else {
            if (selectedTraces.isEmpty()) return;
            List<Trace> ordered = new ArrayList<>();
            for (Trace t : traces)
                if (selectedTraces.contains(t)) ordered.add(t);
            String text = Trace.serializeMultiple(ordered);
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new StringSelection(text), null);
            traces.removeAll(selectedTraces);
            selectedTraces.clear();
            refreshTraceList();
        }
        getActiveListPanel().requestFocusInWindow();
    }

    private void deleteSelected() {
        if (isVariableTabActive()) {
            if (selectedVariables.isEmpty()) return;
            variables.removeAll(selectedVariables);
            selectedVariables.clear();
            refreshVariableList();
        } else if (isWallTabActive()) {
            if (selectedWalls.isEmpty()) return;
            walls.removeAll(selectedWalls);
            selectedWalls.clear();
            refreshWallList();
        } else {
            if (selectedTraces.isEmpty()) return;
            traces.removeAll(selectedTraces);
            selectedTraces.clear();
            refreshTraceList();
        }
        getActiveListPanel().requestFocusInWindow();
    }

    private void selectAll() {
        if (isVariableTabActive()) {
            selectedVariables.clear();
            selectedVariables.addAll(variables);
            refreshVariableList();
        } else if (isWallTabActive()) {
            selectedWalls.clear();
            selectedWalls.addAll(walls);
            refreshWallList();
        } else {
            selectedTraces.clear();
            selectedTraces.addAll(traces);
            refreshTraceList();
        }
        getActiveListPanel().requestFocusInWindow();
    }

    private void deselectAll() {
        selectedTraces.clear();
        selectedWalls.clear();
        selectedVariables.clear();
        refreshTraceList();
        refreshWallList();
        refreshVariableList();
    }

    /** 通用的列表项拖拽排序状态机。 */
    private class DragState<T extends ListItem> {
        private int dragIndex = -1;
        private TraceItemPanel dragPanel;
        private final List<T> items;
        private final DropListPanel panel;
        private final Runnable onChanged;

        DragState(List<T> items, DropListPanel panel, Runnable onChanged) {
            this.items = items;
            this.panel = panel;
            this.onChanged = onChanged;
        }

        void reset() {
            dragIndex = -1;
            dragPanel = null;
        }

        void start(int idx) {
            dragIndex = idx;
            if (idx >= 0 && idx < panel.getComponentCount()) {
                Component c = panel.getComponent(idx);
                if (c instanceof TraceItemPanel p) dragPanel = p;
            }
        }

        private int screenYToIndex(int screenY) {
            Point pt = new Point(0, screenY);
            SwingUtilities.convertPointFromScreen(pt, panel);
            return Math.clamp((pt.y + ITEM_HEIGHT / 2) / ITEM_HEIGHT, 0, items.size());
        }

        void update() {
            if (dragIndex < 0 || dragPanel == null) return;
            int targetIdx = screenYToIndex(dragPanel.dragCurrentScreenY);
            panel.dropLineY = targetIdx * ITEM_HEIGHT;
            panel.paintImmediately(panel.getBounds());
        }

        void end() {
            if (dragIndex < 0 || dragPanel == null) {
                reset();
                panel.dropLineY = -1;
                panel.repaint();
                return;
            }
            int newIndex = screenYToIndex(dragPanel.dragCurrentScreenY);
            newIndex -= newIndex > dragIndex ? 1 : 0;
            if (newIndex != dragIndex) {
                T moved = items.remove(dragIndex);
                items.add(newIndex, moved);
            }
            onChanged.run();
        }
    }

    /** 可绘制落点指示线的列表面板。 */
    private class DropListPanel extends JPanel {
        int dropLineY = -1;

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (dropLineY >= 0) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                try {
                    g2.setColor(Color.decode(Main.ACCENT_COLOR));
                } catch (Exception e) {
                    g2.setColor(new Color(0x26, 0x75, 0xBF));
                }
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(0, dropLineY - 1, getWidth() - 4, dropLineY - 1);
                g2.fillOval(-2, dropLineY - 3, 5, 5);
                g2.dispose();
            }
        }
    }
}
