package org.example.GUI;

import org.example.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/** 单位信息面板：左侧文本信息 + 右上角贴图（绝对定位）。 */
public class UnitInfoPanel extends JPanel {
    private final JLabel nameLabel;
    private final JLabel hpLabel;
    private final JLabel cdLabel;
    private final JLabel atLabel;
    private final JLabel costLabel;
    private final JLabel coordLabel;
    private final JLabel delayLabel;
    private final SpritePanel spritePanel;
    private final JPanel textPanel;

    private static final int SPRITE_SIZE = 60;
    private static final int SPRITE_MARGIN = 4;

    public UnitInfoPanel() {
        super(null);
        setOpaque(true);

        // 文本信息
        textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4 + SPRITE_SIZE + SPRITE_MARGIN));

        nameLabel = new JLabel();
        nameLabel.setFont(new Font("黑体", Font.BOLD, 14));
        textPanel.add(nameLabel);
        textPanel.add(Box.createVerticalStrut(4));

        hpLabel = createInfoLabel();
        cdLabel = createInfoLabel();
        atLabel = createInfoLabel();
        costLabel = createInfoLabel();
        coordLabel = createInfoLabel();
        delayLabel = createInfoLabel();

        textPanel.add(hpLabel);
        textPanel.add(cdLabel);
        textPanel.add(atLabel);
        textPanel.add(costLabel);
        textPanel.add(Box.createVerticalStrut(4));
        textPanel.add(coordLabel);
        textPanel.add(delayLabel);
        textPanel.add(Box.createVerticalGlue());

        add(textPanel);

        // 右上角贴图（绝对定位，需后加入以保证在上层）
        spritePanel = new SpritePanel();
        add(spritePanel);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                int w = getWidth();
                int h = getHeight();
                textPanel.setBounds(0, 0, w, h);
                spritePanel.setBounds(w - SPRITE_SIZE - SPRITE_MARGIN, SPRITE_MARGIN, SPRITE_SIZE, SPRITE_SIZE);
            }
        });
    }

    private JLabel createInfoLabel() {
        JLabel label = new JLabel();
        label.setFont(new Font("黑体", Font.PLAIN, 13));
        return label;
    }

    public void showUnit(Unit unit) {
        Unit.Info info = Unit.infos[unit.id];

        char pskeyChar = Main.pskey.charAt(unit.id % 61 + unit.id / 61);
        nameLabel.setText(info.name() + " (" + pskeyChar + ")");

        int maxHp = info.hp();
        if (unit.hp == maxHp) {
            hpLabel.setText("hp: " + maxHp);
        } else {
            hpLabel.setText("hp: " + unit.hp + "/" + maxHp);
        }

        cdLabel.setText("cd: " + (info.cd() < 0 ? "-" : info.cd()));
        atLabel.setText("at: " + (info.at() < 0 ? "-" : info.at()));
        costLabel.setText("cost: " + (info.cost() <= 0 ? "-" : info.cost() + "￥"));
        String d = info.cd() <= 0 ? "-" : Integer.toString(unit.getDelay());
        if (unit.id == 19){
            d += "(" + (unit.x + 6) % info.cd() + ", " + (60-(unit.x + 6) % info.cd()) + ")";
        }
        coordLabel.setText("x: " + unit.x + " y: " + unit.y + " r: " + unit.r);
        delayLabel.setText("delay: " + d);

        spritePanel.setUnit(unit);
    }

    public void updateDarkMode() {
        spritePanel.updateDarkMode();
    }

    public void clear() {
        nameLabel.setText("");
        hpLabel.setText("");
        cdLabel.setText("");
        atLabel.setText("");
        costLabel.setText("");
        coordLabel.setText("");
        delayLabel.setText("");
        spritePanel.setUnit(null);
    }
}
