package org.example.GUI;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.Main;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;

/** 单位图鉴右侧详情面板：大贴图 + 基础信息 + 最速行动计算 + JSON 扩展区。 */
public class UnitDexDetailPanel extends JPanel {

    private final SpritePanel spritePanel;
    private final JPanel headerPanel;
    private final JPanel infoPanel;
    private final JPanel quickestPanel;
    private final JTextField wallXField;
    private final JLabel wallXLabel;
    private final JCheckBox isFirstCheck;
    private final JTextArea quickestResultArea;
    private final javax.swing.border.TitledBorder quickestBorder;
    private final JScrollPane extScroll;
    private final JLabel nameLabel;
    private final JLabel codeLabel;
    private final JLabel costLabel;
    private final JLabel techLabel;
    private final JLabel hpLabel;
    private final JLabel cdLabel;
    private final JLabel atLabel;
    private final JTextArea extArea;

    private final JsonNode unitDetailJson;
    private int currentUnitId = -1;

    private static final int SPRITE_SIZE = 100;

    public UnitDexDetailPanel(JsonNode unitDetailJson) {
        super(null);
        this.unitDetailJson = unitDetailJson;
        setPreferredSize(new Dimension(480, 400));

        spritePanel = new SpritePanel();
        spritePanel.setBorder(BorderFactory.createEmptyBorder());
        add(spritePanel);

        headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);
        nameLabel = new JLabel();
        nameLabel.setFont(new Font("黑体", Font.BOLD, 16));
        headerPanel.add(nameLabel);
        headerPanel.add(Box.createVerticalStrut(2));
        codeLabel = createDetailLabel();
        headerPanel.add(codeLabel);
        add(headerPanel);

        costLabel = createDetailLabel();
        techLabel = createDetailLabel();
        hpLabel = createDetailLabel();
        cdLabel = createDetailLabel();
        atLabel = createDetailLabel();

        infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 2));
        infoPanel.setOpaque(false);
        infoPanel.add(costLabel);
        infoPanel.add(hpLabel);
        infoPanel.add(cdLabel);
        infoPanel.add(atLabel);
        infoPanel.add(techLabel);
        add(infoPanel);

        // 最速行动计算子面板
        quickestBorder = BorderFactory.createTitledBorder("最速行动计算");
        quickestPanel = new JPanel();
        quickestPanel.setLayout(new BoxLayout(quickestPanel, BoxLayout.Y_AXIS));
        quickestPanel.setOpaque(false);
        quickestPanel.setBorder(quickestBorder);

        JPanel wallRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        wallRow.setOpaque(false);
        wallXLabel = new JLabel("突击壁x:");
        wallRow.add(wallXLabel);
        wallXField = new JTextField(4);
        wallXField.setFont(new Font("黑体", Font.PLAIN, 13));
        wallRow.add(wallXField);
        quickestPanel.add(wallRow);

        isFirstCheck = new JCheckBox("兵玉代码在前");
        isFirstCheck.setFont(new Font("黑体", Font.PLAIN, 12));
        isFirstCheck.setOpaque(false);

        JPanel checkRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        checkRow.setOpaque(false);
        checkRow.add(isFirstCheck);
        quickestPanel.add(checkRow);

        quickestResultArea = new JTextArea();
        quickestResultArea.setEditable(false);
        quickestResultArea.setFont(new Font("黑体", Font.PLAIN, 12));
        quickestResultArea.setLineWrap(true);
        quickestResultArea.setWrapStyleWord(true);
        quickestResultArea.setOpaque(false);
        JScrollPane resultScroll = new JScrollPane(quickestResultArea);
        resultScroll.setBorder(null);
        resultScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        quickestPanel.add(resultScroll);

        quickestPanel.setVisible(false);
        add(quickestPanel);

        // 输入变化时自动重算
        wallXField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { updateQuickest(); }
            @Override public void removeUpdate(DocumentEvent e) { updateQuickest(); }
            @Override public void changedUpdate(DocumentEvent e) { updateQuickest(); }
        });
        isFirstCheck.addActionListener(e -> updateQuickest());

        extArea = new JTextArea();
        extArea.setEditable(false);
        extArea.setFont(new Font("黑体", Font.PLAIN, 13));
        extArea.setLineWrap(true);
        extArea.setWrapStyleWord(true);
        extArea.setOpaque(true);
        extScroll = new JScrollPane(extArea);
        extScroll.setBorder(BorderFactory.createTitledBorder("详细资料"));
        extScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(extScroll);
    }

    @Override
    public void doLayout() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        spritePanel.setBounds(10, 10, SPRITE_SIZE, SPRITE_SIZE);

        int infoX = SPRITE_SIZE + 20;
        int rightW = w - infoX - 10;

        if (quickestPanel.isVisible()) {
            int leftW = rightW * 70 / 100;
            int rightColW = rightW - leftW - 10;

            int headerH = headerPanel.getPreferredSize().height;
            headerPanel.setBounds(infoX, 10, leftW, headerH);

            int infoY = 10 + headerH + 4;
            Dimension infoPref = infoPanel.getPreferredSize();
            infoPanel.setBounds(infoX, infoY, leftW, infoPref.height);

            quickestPanel.setBounds(infoX + leftW + 10, 10, rightColW, SPRITE_SIZE);

            int extY = Math.max(10 + SPRITE_SIZE + 10, infoY + infoPref.height + 10);
            extScroll.setBounds(10, extY, w - 20, h - extY - 10);
        } else {
            int headerH = headerPanel.getPreferredSize().height;
            headerPanel.setBounds(infoX, 10, rightW, headerH);

            int infoY = 10 + headerH + 4;
            Dimension infoPref = infoPanel.getPreferredSize();
            infoPanel.setBounds(infoX, infoY, rightW, infoPref.height);

            int extY = Math.max(10 + SPRITE_SIZE + 10, infoY + infoPref.height + 10);
            extScroll.setBounds(10, extY, w - 20, h - extY - 10);
        }
    }

    private JLabel createDetailLabel() {
        JLabel label = new JLabel();
        label.setFont(new Font("黑体", Font.PLAIN, 13));
        return label;
    }

    public void showUnit(int id) {
        if (id < 0 || id >= Unit.infos.length) {
            clear();
            return;
        }
        currentUnitId = id;
        Unit.Info info = Unit.infos[id];
        Unit unit = new Unit(id, 0, 0, 0);
        spritePanel.setUnit(unit);

        char pskeyChar = id < 61 ? Main.pskey.charAt(id % 61) : Main.pskey.charAt(id - 61 + id / 61);
        nameLabel.setText(info.name() + String.format("  №%02d", id));
        codeLabel.setText("编码: " + pskeyChar);

        costLabel.setText("军资金: " + (info.cost() <= 0 ? "-" : info.cost() + "￥"));
        techLabel.setText("解锁关卡: " + (info.tech() < 0 ? "-" : String.valueOf(info.tech())));
        hpLabel.setText("生命值: " + (info.hp() < 0 ? "-" : String.valueOf(info.hp())));
        cdLabel.setText("冷却: " + (info.cd() < 0 ? "-" : info.cd()));
        atLabel.setText("行动时间: " + (info.at() < 0 ? "-" : String.valueOf(info.at())));

        // 最速行动面板：仅在 cd>=0 && shoot>=0 && 非墙壁类单位时显示
        boolean showQuickest = info.cd() >= 0 && info.shoot() >= 0 && !Unit.isWallLike(id);
        quickestPanel.setVisible(showQuickest);
        if (showQuickest) {
            updateQuickest();
        }

        String ext = buildExtendedInfo(id);
        extArea.setText(ext.isEmpty() ? "详细资料待补全" : ext);
        extArea.setCaretPosition(0);

        updateColors();
        revalidate();
    }

    private void updateQuickest() {
        if (currentUnitId < 0) return;
        String wallText = wallXField.getText().trim();
        if (wallText.isEmpty()) {
            quickestResultArea.setText("");
            return;
        }
        try {
            int wallX = Integer.parseInt(wallText);
            boolean isFirst = isFirstCheck.isSelected();
            List<Integer> xs = Unit.getQuickestXList(currentUnitId, wallX, isFirst);
            if (xs.isEmpty()) {
                quickestResultArea.setText("无可行x坐标");
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < xs.size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(xs.get(i));
                }
                quickestResultArea.setText("x: " + sb);
            }
        } catch (NumberFormatException e) {
            quickestResultArea.setText("请输入有效整数");
        }
    }

    private String buildExtendedInfo(int id) {
        StringBuilder sb = new StringBuilder();
        if (unitDetailJson != null) {
            JsonNode units = unitDetailJson.get("units");
            if (units != null && units.isArray()) {
                for (JsonNode u : units) {
                    if (u.get("id").asInt() == id) {
                        appendField(sb, "介绍", u.get("description"));
                        appendField(sb, "进阶", u.get("tactics"));
                        appendField(sb, "备注", u.get("notes"));
                        break;
                    }
                }
            }
        }
        return sb.toString();
    }

    private void appendField(StringBuilder sb, String label, JsonNode node) {
        if (node != null && !node.asText().isEmpty()) {
            if (!sb.isEmpty()) sb.append("\n\n");
            sb.append("【").append(label).append("】\n").append(node.asText());
        }
    }

    public void updateDarkMode() {
        spritePanel.updateDarkMode();
        updateColors();
    }

    private void updateColors() {
        Color fg = Main.DARK_MODE ? new Color(220, 220, 220) : Color.BLACK;
        Color bg = Main.DARK_MODE ? new Color(43, 43, 43) : Color.WHITE;

        nameLabel.setForeground(fg);
        codeLabel.setForeground(Main.DARK_MODE ? new Color(160, 160, 160) : new Color(120, 120, 120));
        costLabel.setForeground(fg);
        techLabel.setForeground(fg);
        hpLabel.setForeground(fg);
        cdLabel.setForeground(fg);
        atLabel.setForeground(fg);

        quickestBorder.setTitleColor(fg);
        wallXLabel.setForeground(fg);
        quickestResultArea.setForeground(fg);
        quickestResultArea.setBackground(bg);
        quickestResultArea.setCaretColor(fg);
        wallXField.setForeground(fg);
        wallXField.setBackground(bg);
        wallXField.setCaretColor(fg);
        isFirstCheck.setForeground(fg);

        extArea.setForeground(fg);
        extArea.setBackground(bg);
        extArea.setCaretColor(fg);

        setBackground(bg);
    }

    public void clear() {
        currentUnitId = -1;
        nameLabel.setText("");
        codeLabel.setText("");
        costLabel.setText("");
        techLabel.setText("");
        hpLabel.setText("");
        cdLabel.setText("");
        atLabel.setText("");
        extArea.setText("");
        quickestResultArea.setText("");
        wallXField.setText("");
        quickestPanel.setVisible(false);
        spritePanel.setUnit(null);
    }
}
