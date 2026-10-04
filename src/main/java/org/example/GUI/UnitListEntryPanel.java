package org.example.GUI;

import org.example.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/** 单位列表中的单行条目面板。支持点击选中和高亮。 */
public class UnitListEntryPanel extends JPanel {
    private final int unitIndex;
    private boolean selected;

    public UnitListEntryPanel(Unit unit, int index, Consumer<Integer> onSelect) {
        super(new BorderLayout(12, 0));
        this.unitIndex = index;

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Main.DARK_MODE ? new Color(80, 80, 80) : new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        String unitName = Unit.infos[unit.id].name();
        JLabel nameLabel = new JLabel(unitName);
        nameLabel.setFont(new Font("黑体", Font.PLAIN, 13));

        JLabel coordLabel = new JLabel(unit.getLabel());
        coordLabel.setFont(new Font("黑体", Font.PLAIN, 12));
        coordLabel.setForeground(Main.DARK_MODE ? new Color(180, 180, 180) : new Color(120, 120, 120));

        JPanel leftP = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftP.setOpaque(false);
        leftP.add(nameLabel);
        leftP.add(coordLabel);

        add(leftP, BorderLayout.CENTER);

        if (unit.isCore()) {
            JLabel coreTag = new JLabel("¤");
            coreTag.setFont(new Font("黑体", Font.BOLD, 11));
            add(coreTag, BorderLayout.EAST);
            setOpaque(true);
        }

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                onSelect.accept(index);
            }
        });
    }

    public int getUnitIndex() { return unitIndex; }

    public void setSelected(boolean s) {
        if (selected == s) return;
        selected = s;
        Color lineColor;
        if (s) {
            try {
                lineColor = Color.decode(Main.ACCENT_COLOR);
            } catch (Exception e) {
                lineColor = new Color(0x26, 0x75, 0xBF);
            }
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(lineColor, 2),
                    BorderFactory.createEmptyBorder(5, 9, 5, 9)));
        } else {
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Main.DARK_MODE ? new Color(80, 80, 80) : new Color(200, 200, 200), 1),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        }
    }
}
