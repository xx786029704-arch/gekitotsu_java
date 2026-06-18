package org.example.GUI;

import org.example.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** 工作流节点的默认 JPanel 渲染组件。支持悬停、点击选中、长按拖拽。 */
public class NodeComponent extends JPanel {
    private static final Color BG_NORMAL_DARK    = new Color(60, 63, 65);
    private static final Color BG_NORMAL_LIGHT   = new Color(245, 245, 245);
    private static final Color BG_HOVER_DARK     = new Color(77, 77, 77);
    private static final Color BG_HOVER_LIGHT    = new Color(225, 225, 225);
    private static final Color BG_SELECTED_DARK  = new Color(45, 80, 110);
    private static final Color BG_SELECTED_LIGHT = new Color(200, 220, 245);
    private static final Color BG_DISABLED_DARK  = new Color(50, 50, 50);
    private static final Color BG_DISABLED_LIGHT = new Color(230, 230, 230);

    protected final WorkflowNode node;
    protected final Runnable onChanged;
    private final Runnable onDelete;
    private final Runnable onSelect;
    private final Runnable onDragStart;
    private final Runnable onDragUpdate;
    private final Runnable onDragEnd;
    private final JLabel nameLabel;

    private boolean selected;
    private boolean hovered;
    boolean dragging;
    int dragCurrentScreenY;
    private int dragOriginScreenY;
    private final Timer longPressTimer;
    private final JLabel effectSwitch;

    public NodeComponent(WorkflowNode node, Runnable onChanged, Runnable onDelete,
                         Runnable onSelect, Runnable onDragStart, Runnable onDragUpdate,
                         Runnable onDragEnd) {
        super(new BorderLayout(10, 0));
        this.node = node;
        this.onChanged = onChanged;
        this.onDelete = onDelete;
        this.onSelect = onSelect;
        this.onDragStart = onDragStart;
        this.onDragUpdate = onDragUpdate;
        this.onDragEnd = onDragEnd;

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Main.DARK_MODE ? new Color(90, 90, 90) : new Color(180, 180, 180), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        effectSwitch = new JLabel("●");
        effectSwitch.setFont(new Font("黑体", Font.PLAIN, 14));
        effectSwitch.setForeground(new Color(150, 150, 150));
        add(effectSwitch, BorderLayout.WEST);

        nameLabel = new JLabel(node.effect.getName());
        nameLabel.setFont(new Font("黑体", Font.PLAIN, 13));
        add(nameLabel, BorderLayout.CENTER);

        // 右键菜单
        JPopupMenu menu = new JPopupMenu();
        JMenuItem toggleItem = new JMenuItem(node.enabled ? "禁用" : "启用");
        toggleItem.addActionListener(e -> {
            node.enabled = !node.enabled;
            repaint();
            onChanged.run();
        });
        menu.add(toggleItem);
        JMenuItem paramItem = new JMenuItem("编辑参数");
        paramItem.addActionListener(e -> showParameterDialog());
        menu.add(paramItem);
        JMenuItem deleteItem = new JMenuItem("删除");
        deleteItem.addActionListener(e -> onDelete.run());
        menu.add(deleteItem);
        setComponentPopupMenu(menu);

        // 长按拖拽计时器
        longPressTimer = new Timer(350, e -> {
            if (!dragging && getMousePosition() != null) {
                dragging = true;
                onDragStart.run();
                repaint();
            }
        });
        longPressTimer.setRepeats(false);

        // 鼠标事件（只处理按压/拖拽/点击，悬停由 paintComponent 通过 getMousePosition() 判断）
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                dragOriginScreenY = e.getLocationOnScreen().y;
                longPressTimer.restart();
                if (onSelect != null) onSelect.run();
                repaint();
            }
            @Override public void mouseReleased(MouseEvent e) {
                longPressTimer.stop();
                if (dragging) {
                    dragCurrentScreenY = e.getLocationOnScreen().y;
                    dragging = false;
                    onDragEnd.run();
                }
                repaint();
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showParameterDialog();
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!dragging) {
                    int delta = e.getLocationOnScreen().y - dragOriginScreenY;
                    if (Math.abs(delta) < 6) return;
                    dragging = true;
                    longPressTimer.stop();
                    onDragStart.run();
                }
                dragCurrentScreenY = e.getLocationOnScreen().y;
                onDragUpdate.run();
            }
        });

        // 子组件上的拖拽（悬停由 paintComponent 通过 getMousePosition() 判断）
        MouseAdapter childDrag = new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (!dragging) {
                    int delta = e.getLocationOnScreen().y - dragOriginScreenY;
                    if (Math.abs(delta) < 6) return;
                    dragging = true;
                    longPressTimer.stop();
                    onDragStart.run();
                }
                dragCurrentScreenY = e.getLocationOnScreen().y;
                onDragUpdate.run();
            }
        };
        effectSwitch.addMouseMotionListener(childDrag);
        nameLabel.addMouseMotionListener(childDrag);
        // effectSwitch 点击切换启用/禁用，不与拖拽冲突
        effectSwitch.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                dragOriginScreenY = e.getLocationOnScreen().y;
                longPressTimer.restart();
            }
            @Override public void mouseReleased(MouseEvent e) {
                longPressTimer.stop();
                if (dragging) {
                    dragCurrentScreenY = e.getLocationOnScreen().y;
                    dragging = false;
                    onDragEnd.run();
                }
                repaint();
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (!dragging) {
                    node.enabled = !node.enabled;
                    onChanged.run();
                    repaint();
                }
            }
        });
        nameLabel.addMouseListener(ma);

        MouseAdapter hoverRefresher = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e)  { hovered = true; repaint(); }
            @Override public void mouseExited(MouseEvent e)   { hovered = false; repaint(); }
            @Override public void mouseMoved(MouseEvent e)    { hovered = true; repaint(); }
        };
        addMouseListener(hoverRefresher);
        addMouseMotionListener(hoverRefresher);
        effectSwitch.addMouseListener(hoverRefresher);
        nameLabel.addMouseListener(hoverRefresher);
        effectSwitch.addMouseMotionListener(hoverRefresher);
        nameLabel.addMouseMotionListener(hoverRefresher);

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        boolean isHovered = hovered;

        if (!node.enabled) {
            setBackground(Main.DARK_MODE ? BG_DISABLED_DARK : BG_DISABLED_LIGHT);
            nameLabel.setText("<html><span style='color:gray;text-decoration:line-through'>"
                    + node.effect.getName() + "</span></html>");
            effectSwitch.setText("×");
            effectSwitch.setForeground(new Color(200, 80, 80));
        } else if (selected) {
            setBackground(Main.DARK_MODE ? BG_SELECTED_DARK : BG_SELECTED_LIGHT);
            nameLabel.setText(node.effect.getName());
            effectSwitch.setText("●");
            effectSwitch.setForeground(new Color(150, 150, 150));
        } else if (isHovered) {
            setBackground(Main.DARK_MODE ? BG_HOVER_DARK : BG_HOVER_LIGHT);
            nameLabel.setText(node.effect.getName());
            effectSwitch.setText("●");
            effectSwitch.setForeground(new Color(150, 150, 150));
        } else {
            setBackground(Main.DARK_MODE ? BG_NORMAL_DARK : BG_NORMAL_LIGHT);
            nameLabel.setText(node.effect.getName());
            effectSwitch.setText("●");
            effectSwitch.setForeground(new Color(150, 150, 150));
        }
        setOpaque(true);
        super.paintComponent(g);
    }

    void setSelected(boolean s) {
        if (selected != s) {
            selected = s;
            repaint();
        }
    }

    boolean isSelected() { return selected; }

    // -- parameter dialog --

    public void showParameterDialog() {
        java.util.List<EffectParameter> params = node.effect.getParameters();
        if (params.isEmpty()) {
            JOptionPane.showMessageDialog(this, "该效果没有可配置的参数", "参数",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JPanel form = new JPanel(new GridLayout(params.size(), 2, 8, 6));
        java.util.Map<String, JTextField> inputs = new java.util.LinkedHashMap<>();
        for (EffectParameter p : params) {
            form.add(new JLabel(p.label()));
            JTextField field = new JTextField(
                    String.valueOf(node.paramValues.getOrDefault(p.key(), p.defaultValue())));
            inputs.put(p.key(), field);
            form.add(field);
        }

        int result = JOptionPane.showConfirmDialog(this, form,
                "编辑参数 - " + node.effect.getName(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            for (EffectParameter p : params) {
                JTextField field = inputs.get(p.key());
                if (field != null) {
                    String text = field.getText().trim();
                    try {
                        switch (p.type()) {
                            case INT:
                                node.paramValues.put(p.key(), Integer.parseInt(text));
                                break;
                            case STRING:
                                node.paramValues.put(p.key(), text);
                                break;
                            case BOOLEAN:
                                node.paramValues.put(p.key(), Boolean.parseBoolean(text));
                                break;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
            onChanged.run();
        }
    }
}
