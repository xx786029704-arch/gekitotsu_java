package org.example.GUI;

import org.example.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/** 效果库面板：搜索框 + 可双击/拖拽添加的效果条目列表。 */
public class EffectLibraryPanel extends JPanel {
    private final JTextField searchField;
    private final JPanel listPanel;
    private final Consumer<Effect> onAddEffect;

    public EffectLibraryPanel(Consumer<Effect> onAddEffect) {
        super(new BorderLayout(0, 4));
        this.onAddEffect = onAddEffect;

        searchField = new JTextField();
        searchField.setFont(new Font("黑体", Font.PLAIN, 13));
        searchField.putClientProperty("JTextField.placeholderText", "搜索效果...");

        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        add(searchField, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
        });

        refresh();
    }

    public void refresh() {
        listPanel.removeAll();
        List<Effect> results = EffectRegistry.search(searchField.getText());
        for (Effect effect : results) {
            listPanel.add(createEntry(effect));
        }
        listPanel.revalidate();
        listPanel.repaint();
    }

    private JPanel createEntry(Effect effect) {
        var entry = new JPanel(new BorderLayout(6, 0)) {
            boolean hovered, pressed;

            @Override
            protected void paintComponent(Graphics g) {
                if (pressed) {
                    setBackground(Main.DARK_MODE ? new Color(40, 40, 45) : new Color(200, 200, 210));
                } else if (hovered) {
                    setBackground(Main.DARK_MODE ? new Color(70, 70, 75) : new Color(235, 235, 240));
                } else {
                    setBackground(null);
                }
                super.paintComponent(g);
            }
        };
        entry.setOpaque(true);
        entry.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0,
                        Main.DARK_MODE ? new Color(70, 70, 70) : new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        entry.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel nameLabel = new JLabel(effect.getName());
        nameLabel.setFont(new Font("黑体", Font.PLAIN, 12));
        nameLabel.setToolTipText(effect.getDescription());
        nameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        entry.add(nameLabel, BorderLayout.CENTER);

        // 所有鼠标交互注册在 nameLabel 上（因为它覆盖 entry 表面）
        MouseAdapter stateAdapter = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                entry.hovered = true; entry.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                entry.hovered = false; entry.pressed = false; entry.repaint();
            }
            @Override public void mousePressed(MouseEvent e) {
                entry.pressed = true; entry.repaint();
            }
            @Override public void mouseReleased(MouseEvent e) {
                entry.pressed = false; entry.repaint();
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    onAddEffect.accept(effect);
                }
            }
        };
        nameLabel.addMouseListener(stateAdapter);

        // 拖拽支持
        nameLabel.setTransferHandler(new TransferHandler("text") {
            @Override public int getSourceActions(JComponent c) { return COPY; }
            @Override protected Transferable createTransferable(JComponent c) {
                return new StringSelection(effect.getName());
            }
        });
        nameLabel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                TransferHandler th = nameLabel.getTransferHandler();
                th.exportAsDrag(nameLabel, e, TransferHandler.COPY);
            }
        });

        return entry;
    }
}
