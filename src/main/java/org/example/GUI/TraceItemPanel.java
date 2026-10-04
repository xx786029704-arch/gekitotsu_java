package org.example.GUI;

import org.example.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** 轨迹列表条目：色块 + 名称 + 悬停/按压状态。双击编辑，单击色圆切换可见，长按拖拽排序。 */
public class TraceItemPanel extends JPanel {
    private static final Color BG_NORMAL_DARK  = new Color(51, 51, 51);
    private static final Color BG_NORMAL_LIGHT = new Color(255, 255, 255);
    private static final Color BG_HOVER_DARK   = new Color(77, 77, 77);
    private static final Color BG_HOVER_LIGHT  = new Color(230, 230, 230);
    private static final Color BG_PRESS_DARK   = new Color(26, 26, 26);
    private static final Color BG_PRESS_LIGHT  = new Color(205, 205, 205);
    private static final Color BG_DRAG_DARK    = new Color(38, 38, 38);
    private static final Color BG_DRAG_LIGHT   = new Color(240, 240, 245);

    final ListItem item;
    private final Runnable onDoubleClick;
    private final Runnable onToggle;
    private final Runnable onDragStart;
    private final Runnable onDragUpdate;
    private final Runnable onDragEnd;
    private final java.util.function.Consumer<MouseEvent> onSelect;

    private boolean hovered;
    private boolean pressed;
    private boolean selected;
    boolean dragging;
    int dragCurrentScreenY;
    private int dragOriginScreenY;
    private final Timer longPressTimer;

    public TraceItemPanel(ListItem item, Runnable onDoubleClick, Runnable onToggle,
                          Runnable onDragStart, Runnable onDragUpdate, Runnable onDragEnd,
                          java.util.function.Consumer<MouseEvent> onSelect) {
        super(new BorderLayout(6, 0));
        this.item = item;
        this.onDoubleClick = onDoubleClick;
        this.onToggle = onToggle;
        this.onDragStart = onDragStart;
        this.onDragUpdate = onDragUpdate;
        this.onDragEnd = onDragEnd;
        this.onSelect = onSelect;

        boolean isVariable = item instanceof Variable;

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0,
                        Main.DARK_MODE ? new Color(0x55, 0x55, 0x55) : Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, isVariable ? 56 : 36));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (!isVariable) {
            // 颜色圆 —— 作为子组件，事件转发到面板
            Color borderColor = Main.DARK_MODE ? Color.WHITE : Color.BLACK;
            JPanel circle = new JPanel() {
                {
                    setOpaque(false);
                }
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (item.isVisible()) {
                        g2.setColor(item.getColor() != null ? item.getColor() : Color.GRAY);
                        g2.fillOval(2, 4, 16, 16);
                    }
                    g2.setColor(borderColor);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawOval(2, 4, 16, 16);
                    g2.dispose();
                }
                @Override
                public Dimension getPreferredSize() { return new Dimension(20, 20); }
                @Override
                public Dimension getMinimumSize() { return new Dimension(20, 20); }
            };
            // 圆上的事件：压/放转发到面板，松手时若仍在圆形范围内则切换可见性
            circle.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    forwardToParent(e);
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    Point pt = e.getPoint();
                    int dx = pt.x - 10, dy = pt.y - 12;
                    if (dx * dx + dy * dy <= 64) {
                        item.setVisible(!item.isVisible());
                        circle.repaint();
                        onToggle.run();
                    }
                    forwardToParent(e);
                }
            });
            circle.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    forwardToParent(e);
                }
            });
            add(circle, BorderLayout.WEST);
        }

        if (!isVariable) {
            JLabel label = new JLabel(item.getName());
            label.setFont(new Font("黑体", Font.PLAIN, 12));
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e)  { forwardToParent(e); }
                @Override
                public void mouseReleased(MouseEvent e) { forwardToParent(e); }
                @Override
                public void mouseClicked(MouseEvent e)  { forwardToParent(e); }
                @Override
                public void mouseEntered(MouseEvent e)  { forwardToParent(e); }
                @Override
                public void mouseExited(MouseEvent e)   { forwardToParent(e); }
            });
            label.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) { forwardToParent(e); }
            });
            add(label, BorderLayout.CENTER);
        } else {
            // 变量条目：[名称] [滑块(val在上方居中, inf/sub在两端上方)]
            Variable var = (Variable) item;
            JPanel centerPanel = new JPanel(new BorderLayout(1, 0));
            centerPanel.setOpaque(false);

            JLabel nameLabel = new JLabel(var.name);
            nameLabel.setFont(new Font("黑体", Font.PLAIN, 16));
            centerPanel.add(nameLabel, BorderLayout.WEST);

            int min = Math.min(var.inf, var.sub);
            int max = Math.max(var.inf, var.sub);
            int cur = Math.clamp(var.val, min, max);
            JSlider slider = new JSlider(min, max, cur);
            slider.setOpaque(false);
            slider.setFocusable(false);

            JLabel valLabel = new JLabel(String.valueOf(var.val), SwingConstants.CENTER);
            valLabel.setFont(new Font("黑体", Font.PLAIN, 12));

            JLabel infLbl = new JLabel(String.valueOf(var.inf));
            infLbl.setFont(new Font("黑体", Font.PLAIN, 12));
            JLabel subLbl = new JLabel(String.valueOf(var.sub));
            subLbl.setFont(new Font("黑体", Font.PLAIN, 12));

            JPanel labelRow = new JPanel(new BorderLayout());
            labelRow.setOpaque(false);
            labelRow.add(infLbl, BorderLayout.WEST);
            labelRow.add(subLbl, BorderLayout.EAST);
            labelRow.add(valLabel, BorderLayout.CENTER);

            JPanel sliderRow = new JPanel(new BorderLayout());
            sliderRow.setOpaque(false);
            sliderRow.add(labelRow, BorderLayout.NORTH);
            sliderRow.add(slider, BorderLayout.CENTER);

            JPanel sliderArea = new JPanel(new BorderLayout());
            sliderArea.setOpaque(false);
            sliderArea.add(sliderRow, BorderLayout.CENTER);

            centerPanel.add(sliderArea, BorderLayout.CENTER);

            slider.addChangeListener(e -> {
                var.val = slider.getValue();
                valLabel.setText(String.valueOf(var.val));
                onToggle.run();
            });

            // 子组件鼠标事件转发到面板（用于选中/高亮），但不转发拖拽以免干扰滑块操作
            MouseAdapter fwd = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e)  { forwardToParent(e); }
                @Override public void mouseReleased(MouseEvent e) { forwardToParent(e); }
                @Override public void mouseClicked(MouseEvent e)  { forwardToParent(e); }
                @Override public void mouseEntered(MouseEvent e)  { forwardToParent(e); }
                @Override public void mouseExited(MouseEvent e)   { forwardToParent(e); }
            };
            nameLabel.addMouseListener(fwd);
            nameLabel.addMouseMotionListener(new MouseAdapter() {
                @Override public void mouseDragged(MouseEvent e) { forwardToParent(e); }
            });
            slider.addMouseListener(fwd);
            sliderArea.addMouseListener(fwd);
            infLbl.addMouseListener(fwd);
            subLbl.addMouseListener(fwd);
            valLabel.addMouseListener(fwd);

            add(centerPanel, BorderLayout.CENTER);
        }

        // 长按计时器
        longPressTimer = new Timer(350, e -> {
            if (pressed && !dragging) {
                dragging = true;
                onDragStart.run();
                repaint();
            }
        });
        longPressTimer.setRepeats(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                pressed = false;
                if (!dragging) longPressTimer.stop();
                repaint();
            }
            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                dragOriginScreenY = e.getLocationOnScreen().y;
                longPressTimer.restart();
                if (onSelect != null) {
                    onSelect.accept(e);
                }
                repaint();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                longPressTimer.stop();
                if (dragging) {
                    dragging = false;
                    onDragEnd.run();
                }
                repaint();
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    longPressTimer.stop();
                    onDoubleClick.run();
                }
            }
        });
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
    }

    /** 将子组件上的鼠标事件坐标转换为面板坐标后重新投递到面板。 */
    private void forwardToParent(MouseEvent e) {
        MouseEvent converted = SwingUtilities.convertMouseEvent(e.getComponent(), e, this);
        // 根据事件类型调用面板的 process 方法
        int id = converted.getID();
        if (id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_RELEASED
                || id == MouseEvent.MOUSE_CLICKED || id == MouseEvent.MOUSE_ENTERED
                || id == MouseEvent.MOUSE_EXITED) {
            processMouseEvent(converted);
        } else if (id == MouseEvent.MOUSE_DRAGGED || id == MouseEvent.MOUSE_MOVED) {
            processMouseMotionEvent(converted);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (dragging) {
            setBackground(Main.DARK_MODE ? BG_DRAG_DARK : BG_DRAG_LIGHT);
        } else if (selected) {
            setBackground(Main.DARK_MODE ? BG_DRAG_DARK : BG_DRAG_LIGHT);
        } else if (pressed) {
            setBackground(Main.DARK_MODE ? BG_PRESS_DARK : BG_PRESS_LIGHT);
        } else if (hovered) {
            setBackground(Main.DARK_MODE ? BG_HOVER_DARK : BG_HOVER_LIGHT);
        } else {
            setBackground(Main.DARK_MODE ? BG_NORMAL_DARK : BG_NORMAL_LIGHT);
        }
        super.paintComponent(g);
    }

    void setSelected(boolean s) {
        if (selected != s) {
            selected = s;
            repaint();
        }
    }
}
