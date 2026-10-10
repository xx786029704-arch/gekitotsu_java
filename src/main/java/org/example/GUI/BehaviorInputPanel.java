package org.example.GUI;

import org.example.I18n;
import org.example.Setting;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.function.Consumer;

/**
 * 行为分析页的共享输入区：左侧「原始阵型」、右侧「测试阵集」，中间分隔线可拖动。
 *
 * <p>文本变化经 300ms 防抖后刷新两侧信息行，并通过 {@code onChange} 通知各分析项；
 * 需要当前内容时（如启动分析）请用 {@link #snapshot()} 同步读取，不要依赖防抖后的通知。
 */
public class BehaviorInputPanel extends JPanel {

    private final FixedJTextArea playerArea = new FixedJTextArea();
    private final FixedJTextArea evalArea = new FixedJTextArea();
    private final JLabel playerInfoLabel = new JLabel(" ");
    private final JLabel evalInfoLabel = new JLabel(" ");
    private final JButton importPlayerButton = new JButton(I18n.t("behavior.importFrom1p"));
    private final JButton import2pButton = new JButton(I18n.t("behavior.importFrom2p"));
    private final JButton import1pButton = new JButton(I18n.t("behavior.importFrom1p"));
    private final JButton clearEvalButton = new JButton(I18n.t("behavior.clear"));
    private final Timer debounce = new Timer(300, e -> notifyChanged());
    private final Consumer<BehaviorInput> onChange;

    public BehaviorInputPanel(Consumer<BehaviorInput> onChange) {
        super(new BorderLayout());
        this.onChange = onChange;
        setBorder(BorderFactory.createTitledBorder(I18n.t("behavior.input")));
        setPreferredSize(new Dimension(0, 160));
        debounce.setRepeats(false);

        for (FixedJTextArea area : new FixedJTextArea[]{playerArea, evalArea}) {
            area.setFont(I18n.font(Font.PLAIN, 13));
            area.setRows(4);
            area.setLineWrap(true);
        }
        playerInfoLabel.setFont(I18n.font(Font.PLAIN, 12));
        evalInfoLabel.setFont(I18n.font(Font.PLAIN, 12));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildPlayerColumn(), buildEvalColumn());
        split.setResizeWeight(0.5);
        split.setContinuousLayout(true);
        split.setBorder(null);
        SplitRatio.applyOnFirstShow(split, 0.5);
        add(split, BorderLayout.CENTER);

        wireEvents();
        refreshInfo();
    }

    /** 同步读取当前两侧输入（不受防抖影响）。 */
    public BehaviorInput snapshot() {
        return new BehaviorInput(playerArea.getText(), evalArea.getText());
    }

    /** 分析进行中锁定输入区（true），结束后解锁（false）。 */
    public void setLocked(boolean locked) {
        boolean enabled = !locked;
        playerArea.setEnabled(enabled);
        evalArea.setEnabled(enabled);
        importPlayerButton.setEnabled(enabled);
        import2pButton.setEnabled(enabled);
        import1pButton.setEnabled(enabled);
        clearEvalButton.setEnabled(enabled);
    }

    private JPanel buildPlayerColumn() {
        JPanel column = new JPanel(new BorderLayout(0, 4));
        column.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.add(new JLabel(I18n.t("behavior.playerFort")), BorderLayout.WEST);
        header.add(importPlayerButton, BorderLayout.EAST);

        column.add(header, BorderLayout.NORTH);
        column.add(new JScrollPane(playerArea), BorderLayout.CENTER);
        column.add(playerInfoLabel, BorderLayout.SOUTH);
        return column;
    }

    private JPanel buildEvalColumn() {
        JPanel column = new JPanel(new BorderLayout(0, 4));
        column.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(import1pButton);
        buttons.add(import2pButton);
        buttons.add(clearEvalButton);

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.add(new JLabel(I18n.t("behavior.evalSet")), BorderLayout.WEST);
        header.add(buttons, BorderLayout.EAST);

        column.add(header, BorderLayout.NORTH);
        column.add(new JScrollPane(evalArea), BorderLayout.CENTER);
        column.add(evalInfoLabel, BorderLayout.SOUTH);
        return column;
    }

    private void wireEvents() {
        DocumentListener listener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                debounce.restart();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                debounce.restart();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                debounce.restart();
            }
        };
        playerArea.getDocument().addDocumentListener(listener);
        evalArea.getDocument().addDocumentListener(listener);

        importPlayerButton.addActionListener(e -> {
            String first = firstFormationText(BattleTab.readFileAutoEncoding("1P.txt"));
            if (first == null) {
                JOptionPane.showMessageDialog(this, I18n.t("behavior.noFortIn1p"),
                        I18n.t("msg.tip"), JOptionPane.WARNING_MESSAGE);
            } else {
                playerArea.setText(first);
            }
        });
        import2pButton.addActionListener(e -> evalArea.setText(BattleTab.readFileAutoEncoding("2P.txt")));
        import1pButton.addActionListener(e -> evalArea.setText(BattleTab.readFileAutoEncoding("1P.txt")));
        clearEvalButton.addActionListener(e -> evalArea.setText(""));
    }

    private void notifyChanged() {
        refreshInfo();
        if (onChange != null) {
            onChange.accept(snapshot());
        }
    }

    /** 刷新两侧信息行：原始阵型的阵名 / 单位数 / 军资金，测试阵集的解析数量。 */
    private void refreshInfo() {
        BehaviorInput input = snapshot();
        playerInfoLabel.setText(describePlayer(input.playerText()));
        evalInfoLabel.setText(describeEval(input.evalText()));
    }

    private static String describePlayer(String playerText) {
        if (playerText.isEmpty()) {
            return I18n.t("behavior.noPlayer");
        }
        try {
            if (Setting.parseFortsRaw(playerText).size() != 1) {
                return I18n.t("behavior.onlyOne");
            }
            Formation formation = Formation.decode(playerText);
            int units = formation.units.size() - 1;
            int cost = 0;
            for (int i = 1; i <= units; i++) {
                cost += Math.max(Unit.infos[formation.units.get(i).id].cost(), 1);
            }
            return I18n.t("behavior.playerInfo",
                    formation.name.isEmpty() ? I18n.t("common.none") : formation.name, units, cost);
        } catch (Exception ex) {
            return I18n.t("behavior.fortParseFail") + ex.getMessage();
        }
    }

    private static String describeEval(String evalText) {
        if (evalText.isEmpty()) {
            return I18n.t("behavior.noEval");
        }
        try {
            int count = Setting.parseFortsRaw(evalText).size();
            return count == 0 ? I18n.t("behavior.noEvalParsed") : I18n.t("behavior.evalParsed", count);
        } catch (Exception ex) {
            return I18n.t("behavior.evalParseFail") + ex.getMessage();
        }
    }

    /** 返回文本中第一个非空阵型片段（按 "/" 分隔），没有则返回 null。 */
    private static String firstFormationText(String content) {
        for (String part : content.split("/")) {
            part = part.trim();
            if (!part.isEmpty()) {
                return part;
            }
        }
        return null;
    }
}
