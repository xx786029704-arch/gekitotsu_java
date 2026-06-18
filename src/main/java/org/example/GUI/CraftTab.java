package org.example.GUI;

import org.example.Main;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class CraftTab extends JPanel {
    private final JFrame parentFrame;

    private final JScrollPane inputPanel;
    private final JScrollPane outputPanel;
    private final JTabbedPane infoTabPane;
    private final JTextPane fortInfoTextArea;
    private final JPanel unitListContent;
    private final WorkflowDropPanel workflowContent;
    private final EffectLibraryPanel effectLibraryPanel;
    private final FortPreviewPanel fortPreviewPanel;
    private final JPanel unitInfoPanel;
    private final JPanel fortInfoPanel;
    private final AnalysisPanel analysisPanel;

    private final FixedJTextArea inputTextArea;
    private final FixedJTextArea outputTextArea;

    private final Timer parseTimer;
    private final WorkflowGraph workflowGraph = new WorkflowGraph();
    private List<Unit> currentUnits = new ArrayList<>();
    private String currentFortName = "";
    private String selectedNodeId = null;
    private final WorkflowDragState workflowDrag = new WorkflowDragState();

    private final JLabel workflowInputLabel;
    private final JLabel workflowOutputLabel;

    // 单位选择与单位信息面板
    private int selectedUnitIndex = -1;
    private UnitInfoPanel unitInfoContent;

    // 输入输出面板之间的快捷复制按钮
    private final JButton outputToInputBtn;

    public CraftTab(JFrame parentFrame) {
        super(new BorderLayout(8, 8));
        this.parentFrame = parentFrame;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        inputTextArea = createTextArea(true);
        outputTextArea = createTextArea(false);

        inputPanel = new JScrollPane(inputTextArea);
        outputPanel = new JScrollPane(outputTextArea);
        unitInfoPanel = new JPanel(new BorderLayout());
        unitInfoPanel.setBorder(BorderFactory.createTitledBorder("单位信息"));
        unitInfoContent = new UnitInfoPanel();
        unitInfoPanel.add(unitInfoContent, BorderLayout.CENTER);

        fortInfoPanel = new JPanel(new BorderLayout());
        fortPreviewPanel = new FortPreviewPanel();
        fortInfoPanel.add(fortPreviewPanel, BorderLayout.CENTER);
        analysisPanel = new AnalysisPanel();

        inputPanel.setBorder(BorderFactory.createTitledBorder("输入"));
        outputPanel.setBorder(BorderFactory.createTitledBorder("输出"));
        fortInfoPanel.setBorder(BorderFactory.createTitledBorder("阵型预览"));


        // ---- 要塞信息标签页 ----
        fortInfoTextArea = new JTextPane();
        fortInfoTextArea.setEditable(false);
        fortInfoTextArea.setFont(new Font("黑体", Font.PLAIN, 14));

        inputTextArea.setMargin(new Insets(0,0,0,0));
        fortInfoTextArea.setMargin(new Insets(0,0,0,0));

        JScrollPane fortInfoScroll = new JScrollPane(fortInfoTextArea);
        fortInfoScroll.setBorder(null);
        fortInfoScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        fortInfoScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // ---- 单位列表标签页 ----
        unitListContent = new JPanel();
        unitListContent.setLayout(new BoxLayout(unitListContent, BoxLayout.Y_AXIS));
        unitListContent.setOpaque(true);

        JScrollPane unitListScroll = new JScrollPane(unitListContent);
        unitListScroll.setBorder(null);
        unitListScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        unitListScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        unitListScroll.getVerticalScrollBar().setUnitIncrement(16);

        infoTabPane = new JTabbedPane();
        infoTabPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Main.DARK_MODE ? new Color(70, 70, 70) : new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        infoTabPane.addTab("要塞信息", fortInfoScroll);
        infoTabPane.addTab("单位列表", unitListScroll);
        infoTabPane.addChangeListener(e -> {
            if (infoTabPane.getSelectedIndex() != 1) deselectUnit();
        });

        // 点击单位列表空白区域取消选择
        unitListContent.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { deselectUnit(); }
        });

        // ---- 工作台面板 ----
        workflowInputLabel = createWorkflowLabel("▼ 输入");
        workflowOutputLabel = createWorkflowLabel("▼ 输出");

        workflowContent = new WorkflowDropPanel();
        workflowContent.add(workflowInputLabel);
        workflowContent.add(workflowOutputLabel);

        workflowContent.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                selectedNodeId = null;
                updateNodeSelections();
            }
        });

        JScrollPane workflowScroll = new JScrollPane(workflowContent);
        workflowScroll.setBorder(null);
        workflowScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        workflowScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        workflowScroll.getVerticalScrollBar().setUnitIncrement(16);
        workflowScroll.setTransferHandler(new TransferHandler("text") {
            @Override
            public boolean canImport(TransferSupport support) {
                if (!support.isDataFlavorSupported(DataFlavor.stringFlavor)) return false;
                if (support.isDrop()) {
                    Point pt = support.getDropLocation().getDropPoint();
                    int labelH = workflowInputLabel.getHeight() + 4;
                    int nodeH = 38;
                    int maxNodes = workflowGraph.getEffectNodes().size();
                    int idx = Math.max(0, Math.min((pt.y - labelH + nodeH / 2) / nodeH, maxNodes));
                    workflowContent.dropLineY = labelH + idx * nodeH;
                    workflowContent.repaint();
                }
                return true;
            }
            @Override
            public boolean importData(TransferSupport support) {
                workflowContent.dropLineY = -1;
                try {
                    Transferable t = support.getTransferable();
                    String name = (String) t.getTransferData(DataFlavor.stringFlavor);
                    for (Effect e : EffectRegistry.getAll()) {
                        if (e.getName().equals(name)) {
                            Point pt = support.getDropLocation().getDropPoint();
                            int labelH = workflowInputLabel.getHeight() + 4;
                            int nodeH = 38;
                            int maxNodes = workflowGraph.getEffectNodes().size();
                            int insertIdx = Math.max(0, Math.min((pt.y - labelH + nodeH / 2) / nodeH, maxNodes));
                            addEffectToWorkflowAt(e, insertIdx);
                            return true;
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    workflowContent.repaint();
                }
                return false;
            }
        });

        // ---- 效果库面板 ----
        EffectRegistry.loadBuiltins();
        effectLibraryPanel = new EffectLibraryPanel(this::addEffectToWorkflow);

        // 输出→输入 快捷按钮
        outputToInputBtn = new JButton(new ImageIcon(getClass().getResource("/outout_to_input.png")));
        outputToInputBtn.setBorderPainted(false);
        outputToInputBtn.setContentAreaFilled(false);
        outputToInputBtn.setFocusPainted(false);
        outputToInputBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        outputToInputBtn.setToolTipText("将输出内容复制到输入");
        outputToInputBtn.addActionListener(e -> {
            inputTextArea.setText(outputTextArea.getText());
            inputTextArea.setCaretPosition(0);
        });

        // 左侧列：1:1:2 比例
        JPanel leftPanel = new JPanel(null);
        leftPanel.add(inputPanel);
        leftPanel.add(outputPanel);
        leftPanel.add(outputToInputBtn);
        leftPanel.setComponentZOrder(outputToInputBtn, 0);
        leftPanel.add(infoTabPane);
        leftPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = leftPanel.getWidth();
                int h = leftPanel.getHeight();
                if (w <= 0 || h <= 0) return;
                int gap = 2;
                int contentH = h - gap * 6;
                int inputH = contentH / 4;
                int outputH = contentH / 4;
                int infoH = contentH - inputH - outputH;
                int y = gap;
                inputPanel.setBounds(gap, y, w - gap * 2, inputH);
                y += inputH + gap * 2;
                outputPanel.setBounds(gap, y, w - gap * 2, outputH);
                y += outputH + gap * 2;
                infoTabPane.setBounds(gap, y, w - gap * 2, infoH);

                // 输出→输入按钮定位在输入/输出面板分界处右侧
                Dimension btnSize = outputToInputBtn.getPreferredSize();
                outputToInputBtn.setBounds(
                        w - gap * 2 - btnSize.width,
                        inputPanel.getY() + inputPanel.getHeight() + gap - btnSize.height / 4,
                        btnSize.width, btnSize.height);
            }
        });

        // 右侧列：25:45:30 比例
        JPanel rightPanel = new JPanel(null);
        rightPanel.add(unitInfoPanel);
        rightPanel.add(fortInfoPanel);
        rightPanel.add(analysisPanel);
        rightPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = rightPanel.getWidth();
                int h = rightPanel.getHeight();
                if (w <= 0 || h <= 0) return;
                int gap = 2;
                int contentH = h - gap * 6;
                int unitInfoH = contentH * 25 / 100;
                int fortInfoH = contentH * 45 / 100;
                int analysisH = contentH - unitInfoH - fortInfoH;
                int y = gap;
                unitInfoPanel.setBounds(gap, y, w - gap * 2, unitInfoH);
                y += unitInfoH + gap * 2;
                fortInfoPanel.setBounds(gap, y, w - gap * 2, fortInfoH);
                y += fortInfoH + gap * 2;
                analysisPanel.setBounds(gap, y, w - gap * 2, analysisH);
            }
        });

        // 中间列：工作台 + 效果库，固定宽度比
        JPanel centerPanel = new JPanel(null);
        centerPanel.add(workflowScroll);
        centerPanel.add(effectLibraryPanel);
        centerPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = centerPanel.getWidth();
                int h = centerPanel.getHeight();
                if (w <= 0 || h <= 0) return;
                int workflowW = w * 33 / 55;
                int libraryW = w - workflowW;
                workflowScroll.setBounds(0, 0, workflowW, h);
                effectLibraryPanel.setBounds(workflowW, 0, libraryW, h);
            }
        });

        // 三列，固定宽度比
        JPanel columns = new JPanel(null);
        columns.add(leftPanel);
        columns.add(centerPanel);
        columns.add(rightPanel);
        columns.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = columns.getWidth();
                int h = columns.getHeight();
                if (w <= 0 || h <= 0) return;
                int leftW = w * 22 / 100;
                int centerW = w * 55 / 100;
                int rightW = w - leftW - centerW;
                leftPanel.setBounds(0, 0, leftW, h);
                centerPanel.setBounds(leftW, 0, centerW, h);
                rightPanel.setBounds(leftW + centerW, 0, rightW, h);
            }
        });

        add(columns, BorderLayout.CENTER);

        // 自动解析：300ms 防抖
        parseTimer = new Timer(300, e -> runPipeline());
        parseTimer.setRepeats(false);
        inputTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { parseTimer.restart(); }
            @Override public void removeUpdate(DocumentEvent e) { parseTimer.restart(); }
            @Override public void changedUpdate(DocumentEvent e) { parseTimer.restart(); }
        });

        // 全局点击取消单位选择（点击单位列表以外区域）
        Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
            if (e instanceof MouseEvent me && me.getID() == MouseEvent.MOUSE_PRESSED) {
                Component src = me.getComponent();
                if (!SwingUtilities.isDescendingFrom(src, unitListContent)) {
                    deselectUnit();
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    // ========== 深色模式 ==========

    /** 深色模式切换时刷新所有组件样式。 */
    public void updateDarkMode() {
        workflowContent.repaint();
        updateUnitList();
        fortPreviewPanel.repaint();
        effectLibraryPanel.refresh();
        unitInfoContent.updateDarkMode();
    }

    // ========== 工作流操作 ==========

    private void addEffectToWorkflow(Effect effect) {
        addEffectToWorkflowAt(effect, workflowGraph.getEffectNodes().size());
    }

    private void addEffectToWorkflowAt(Effect effect, int insertIndex) {
        WorkflowNode node = new WorkflowNode(effect);
        workflowGraph.addNode(node);

        List<String> order = workflowGraph.getExecutionOrder();
        if (!order.contains(node.id)) {
            order.add(Math.min(insertIndex, order.size()), node.id);
        }
        workflowGraph.rebuildLinearChain(order);
        rebuildWorkflowUI();
        runPipeline();
        // 有参数时才弹出编辑对话框
        if (!effect.getParameters().isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                for (Component c : workflowContent.getComponents()) {
                    if (c instanceof NodeComponent nc && nc.node == node) {
                        nc.showParameterDialog();
                        break;
                    }
                }
            });
        }
    }

    private void removeNodeFromWorkflow(String nodeId) {
        List<String> order = workflowGraph.getExecutionOrder();
        order.remove(nodeId);
        workflowGraph.removeNode(nodeId);
        workflowGraph.rebuildLinearChain(order);
        rebuildWorkflowUI();
        runPipeline();
    }

    private void rebuildWorkflowUI() {
        workflowContent.removeAll();
        workflowContent.add(workflowInputLabel);
        workflowContent.add(Box.createVerticalStrut(4));

        List<String> order = workflowGraph.getExecutionOrder();
        for (String id : order) {
            WorkflowNode node = workflowGraph.getNode(id);
            if (node != null) {
                final int nodeIndex = order.indexOf(id);
                JPanel comp = node.createComponent(
                        () -> { rebuildWorkflowUI(); runPipeline(); },
                        () -> removeNodeFromWorkflow(id),
                        () -> {
                            selectedNodeId = id;
                            // 更新所有节点的选中状态
                            for (Component c : workflowContent.getComponents()) {
                                if (c instanceof NodeComponent nc) {
                                    nc.setSelected(nc.isSelected() || nc == c);
                                }
                            }
                            updateNodeSelections();
                        },
                        () -> workflowDrag.start(nodeIndex),
                        () -> workflowDrag.update(),
                        () -> workflowDrag.end());
                workflowContent.add(comp);
                workflowContent.add(Box.createVerticalStrut(4));
            }
        }
        workflowContent.add(workflowOutputLabel);
        workflowContent.add(Box.createVerticalGlue());
        workflowContent.revalidate();
        workflowContent.repaint();
    }

    // ========== 选中状态同步 ==========

    private void updateNodeSelections() {
        for (Component c : workflowContent.getComponents()) {
            if (c instanceof NodeComponent nc) {
                nc.setSelected(nc.node.id.equals(selectedNodeId));
            }
        }
    }

    // ========== 管线执行 ==========

    private void runPipeline() {
        String text = inputTextArea.getText().trim();
        if (text.isEmpty()) {
            clearResults();
            return;
        }

        try {
            // 解阵
            Formation input = Formation.decode(text);
            // 执行工作流
            Formation result = workflowGraph.execute(input);

            // 限制边界
            for (Unit u : result.units){
                if (u.id < 0 || u.id > 62){
                    u.id = 63;
                }
                u.r = (u.r % 360 + 360) % 360;
                u.x = Math.clamp(u.x, 0, u.isCore() ? 276 : 348);
                u.y = Math.clamp(u.y, 0, u.isCore() ? 276 : 349);
            }
            // 合法性校验

            // 显示结果
            currentUnits = result.units;
            currentFortName = result.name;
            updateFortInfo();
            updateUnitList();
            //分析&建议
            analysisPanel.setUnits(FormationStabilizer.stabilize(currentUnits));
            outputTextArea.setText(result.encode());
            outputTextArea.setCaretPosition(0);
        } catch (Exception e) {
            clearResults();
            fortInfoTextArea.setText("解析失败: " + e.getMessage());
            outputTextArea.setCaretPosition(0);
        }
    }

    private void clearResults() {
        currentUnits.clear();
        currentFortName = "";
        selectedUnitIndex = -1;
        fortInfoTextArea.setText("");
        outputTextArea.setText("");
        analysisPanel.clear();
        unitListContent.removeAll();
        unitListContent.revalidate();
        unitListContent.repaint();
        unitInfoContent.clear();
    }

    // ========== 要塞信息 ==========

    private void updateFortInfo() {
        int accelLevel = 0;
        int totalCost = 0;
        for (Unit u : currentUnits) {
            if (u.id == 29) accelLevel++;
            else if (u.id == 30) accelLevel+=2;
            if (u.id >= 0 && u.id < Unit.infos.length) {
                totalCost += Unit.infos[u.id].cost();
            }
        }

        javax.swing.text.StyledDocument doc = fortInfoTextArea.getStyledDocument();
        javax.swing.text.SimpleAttributeSet normal = new javax.swing.text.SimpleAttributeSet();
        javax.swing.text.StyleConstants.setFontFamily(normal, "黑体");
        javax.swing.text.StyleConstants.setFontSize(normal, 14);
        javax.swing.text.StyleConstants.setForeground(normal, fortInfoTextArea.getForeground());

        javax.swing.text.SimpleAttributeSet costAttr = new javax.swing.text.SimpleAttributeSet(normal);
        javax.swing.text.StyleConstants.setForeground(costAttr,
                totalCost > 1500 ? new Color(240, 70, 70) : new Color(70, 210, 70));
        javax.swing.text.StyleConstants.setBold(costAttr, true);

        try {
            doc.remove(0, doc.getLength());
            doc.insertString(doc.getLength(), "\n 阵型名称: " + (currentFortName.isEmpty() ? "(无)" : currentFortName) + "\n\n", normal);
            doc.insertString(doc.getLength(), " 加速等级: " + accelLevel + "\n\n", normal);
            doc.insertString(doc.getLength(), " 总军资金: ", normal);
            doc.insertString(doc.getLength(), String.valueOf(totalCost), costAttr);
            doc.insertString(doc.getLength(), "\n", normal);
        } catch (javax.swing.text.BadLocationException ex) {
            fortInfoTextArea.setText("\n 阵型名称: " + currentFortName + "\n\n 加速等级: " + accelLevel + "\n\n 总军资金: " + totalCost);
        }
    }

    // ========== 单位列表 ==========

    private void updateUnitList() {
        unitListContent.removeAll();
        deselectUnit();

        for (int i = 0; i < currentUnits.size(); i++) {
            Unit unit = currentUnits.get(i);
            JPanel entry = createUnitEntry(unit, i);
            unitListContent.add(entry);
            if (i < currentUnits.size() - 1) {
                unitListContent.add(Box.createVerticalStrut(2));
            }
        }
        unitListContent.add(Box.createVerticalGlue());
        unitListContent.revalidate();
        unitListContent.repaint();
        fortPreviewPanel.setFormation(currentFortName, currentUnits);
    }

    private UnitListEntryPanel createUnitEntry(Unit unit, int index) {
        return new UnitListEntryPanel(unit, index, this::selectUnit);
    }

    private void selectUnit(int index) {
        if (index < 0 || index >= currentUnits.size()) return;
        selectedUnitIndex = index;
        // 更新选中高亮
        updateEntrySelection();
        // 更新单位信息面板
        unitInfoContent.showUnit(currentUnits.get(index));
    }

    private void deselectUnit() {
        if (selectedUnitIndex < 0) return;
        selectedUnitIndex = -1;
        updateEntrySelection();
    }

    /** 遍历单位列表条目，同步选中高亮样式。 */
    private void updateEntrySelection() {
        for (Component c : unitListContent.getComponents()) {
            if (c instanceof UnitListEntryPanel entry) {
                entry.setSelected(entry.getUnitIndex() == selectedUnitIndex);
            }
        }
    }

    // ========== 布局辅助 ==========

    private JLabel createWorkflowLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("黑体", Font.BOLD, 12));
        label.setForeground(Main.DARK_MODE ? new Color(160, 160, 160) : new Color(100, 100, 100));
        label.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        label.setAlignmentX(CENTER_ALIGNMENT);
        return label;
    }

private static FixedJTextArea createTextArea(boolean editable) {
        FixedJTextArea area = new FixedJTextArea(5, 20);
        area.setEditable(editable);
        area.setFont(new Font("黑体", Font.PLAIN, 13));
        area.setTabSize(4);
        area.setLineWrap(true);
        area.setWrapStyleWord(false);
        return area;
    }

    /** 工作台节点拖拽排序状态机。 */
    private class WorkflowDragState {
        private int dragIndex = -1;
        private NodeComponent dragPanel;

        void reset() {
            dragIndex = -1;
            dragPanel = null;
            workflowContent.dropLineY = -1;
            workflowContent.repaint();
        }

        void start(int idx) {
            dragIndex = idx;
            int compIdx = 2 + idx * 2; // label(0), strut(1), node0(2), strut(3), node1(4), ...
            if (compIdx < workflowContent.getComponentCount()) {
                Component c = workflowContent.getComponent(compIdx);
                if (c instanceof NodeComponent p) dragPanel = p;
            }
        }

        private int screenYToIndex(int screenY) {
            Point pt = new Point(0, screenY);
            SwingUtilities.convertPointFromScreen(pt, workflowContent);
            int labelH = workflowInputLabel.getHeight() + 4;
            int relY = pt.y - labelH;
            int nodeH = 38;
            return Math.max(0, Math.min((relY + nodeH / 2) / nodeH, workflowGraph.getEffectNodes().size()));
        }

        void update() {
            if (dragIndex < 0 || dragPanel == null) return;
            int targetIdx = screenYToIndex(dragPanel.dragCurrentScreenY);
            int labelH = workflowInputLabel.getHeight() + 4;
            workflowContent.dropLineY = labelH + targetIdx * 38;
            workflowContent.repaint();
        }

        void end() {
            if (dragIndex < 0 || dragPanel == null) {
                reset();
                return;
            }
            int newIndex = screenYToIndex(dragPanel.dragCurrentScreenY);
            newIndex = Math.min(newIndex, workflowGraph.getEffectNodes().size());
            List<String> order = workflowGraph.getExecutionOrder();
            if (dragIndex < order.size() && newIndex != dragIndex && newIndex != dragIndex + 1) {
                String moved = order.remove(dragIndex);
                int insertAt = newIndex > dragIndex ? newIndex - 1 : newIndex;
                order.add(insertAt, moved);
                workflowGraph.rebuildLinearChain(order);
                rebuildWorkflowUI();
                runPipeline();
            }
            reset();
        }
    }

}
