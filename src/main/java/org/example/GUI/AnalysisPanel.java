package org.example.GUI;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class AnalysisPanel extends JPanel {
    private final JTextPane textPane;

    private List<Unit> units;
    private List<AssaultGroup> assaultGroups;

    private static final Color LEVEL_2_COLOR = new Color(80, 170, 240);
    private static final Color LEVEL_3_COLOR = new Color(210, 170, 0);
    private static final Color LEVEL_4_COLOR = new Color(220, 60, 60);

    public AnalysisPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("分析"));

        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setFont(new Font("黑体", Font.PLAIN, 14));
        textPane.setMargin(new Insets(0, 0, 0, 0));

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
    }

    /** 设置待分析的单位列表，同时自动检测突击组并重新分析。 */
    public void setUnits(List<Unit> u) {
        units = u;
        assaultGroups = AssaultDetector.detect(units);
        refresh();
    }

    public void clear() {
        textPane.setText("");
    }

    // ============ 工作流调度 ============

    private void refresh() {
        textPane.setText("");
        if (units == null || units.size() < 2) return;

        StyledDocument doc = textPane.getStyledDocument();
        SimpleAttributeSet base = mkStyle(null);
        SimpleAttributeSet level2 = mkStyle(LEVEL_2_COLOR);
        SimpleAttributeSet level3 = mkStyle(LEVEL_3_COLOR);
        SimpleAttributeSet level4 = mkStyle(LEVEL_4_COLOR);

        checkOverlap(doc, base, level2, level3, level4);
        checkAssaultQuickest(doc, base, level3);
        checkWallJade(doc, level3);
        checkGunJadeAngle(doc, level3);
        checkHealerRepairPosition(doc, level3);
    }

    // ============ 检测项 ============

    /** 检测同种兵玉坐标过近 / 完全重叠。 */
    private void checkOverlap(StyledDocument doc,
                              SimpleAttributeSet base,
                              SimpleAttributeSet level2,
                              SimpleAttributeSet level3,
                              SimpleAttributeSet level4) {

        for (int i = 0; i < units.size(); i++) {
            Unit a = units.get(i);
            if (a.id == 29 || a.id == 30 || a.id == 23 || a.id == 34 || a.id == 38 || a.id == 40 || a.id == 44)
                continue;
            for (int j = i + 1; j < units.size(); j++) {
                Unit b = units.get(j);
                if (a.id != b.id) continue;

                int dx = Math.abs(a.x - b.x);
                int dy = Math.abs(a.y - b.y);
                int dr = Math.abs(a.r - b.r);
                Unit.Info info = Unit.infos[a.id];

                if (2 * dx + 2 * dy + 2 * dr >= 8) continue;
                int dd = a.getDelay() - b.getDelay();
                dd = dd < 0 ? Math.min(dd + info.cd() + info.at(), -dd)
                            : Math.min(-dd + info.cd() + info.at(), dd);
                if (dd > 3) continue;

                String name = Unit.infos[a.id].name();
                String pos = "(" + a.x + ", " + a.y + ")";
                int severity;
                String message;

                if (dx == 0 && dy == 0 && dr == 0) {
                    severity = 4;
                    message = j == i + 1
                            ? "多个" + name + "在" + pos + "处完全重叠，可能需要修改"
                            : "多个" + name + "在" + pos + "处重叠，可能需要修改";
                } else {
                    if ((a.id == 1 || a.id == 7 || a.id == 17 || a.id == 18) && dd > 1) continue;
                    severity = 3;
                    message = "多个" + name + "在" + pos + "处距离过近，可能需要修改";
                }

                SimpleAttributeSet style = switch (severity) {
                    case 2 -> level2;
                    case 3 -> level3;
                    case 4 -> level4;
                    default -> base;
                };

                appendLine(doc, message, style);
            }
        }
    }

    /** 检测各组突击中每种兵玉是否采用了落地最速 x 坐标。 */
    private void checkAssaultQuickest(StyledDocument doc,
                                      SimpleAttributeSet base,
                                      SimpleAttributeSet suggest) {
        if (assaultGroups == null || assaultGroups.isEmpty()) return;

        List<String> issues = AssaultDetector.checkQuickestX(assaultGroups);
        for (String issue : issues) {
            appendLine(doc, issue, suggest);
        }
    }

    /** 检测壁玉坐标是否位于建议位置。 */
    private void checkWallJade(StyledDocument doc, SimpleAttributeSet suggest) {
        List<Unit> walls = new ArrayList<>();
        for (Unit u : units) {
            if (u.id == 19) walls.add(u);
        }
        if (walls.isEmpty() || walls.size() > 2) return;

        if (walls.size() == 1) {
            Unit w = walls.get(0);
            if (w.x % 60 != 54) {
                appendLine(doc, "壁玉坐标可能需要调整，建议的坐标有[54, 114, 174, 234, 294]", suggest);
            }
        } else {
            for (Unit w : walls) {
                int m = w.x % 60;
                if (m != 43 && m != 45 && m != 53 && m != 55) {
                    appendLine(doc, "壁玉坐标可能需要调整", suggest);
                    return;
                }
            }
        }
    }

    /** 检测枪玉 y=349 时角度是否非 2。 */
    private void checkGunJadeAngle(StyledDocument doc, SimpleAttributeSet suggest) {
        for (Unit u : units) {
            if (u.id != 8 || u.y != 349) continue;
            boolean badAngle = (u.r >= 357 && u.r <= 359) || (u.r >= 0 && u.r <= 1);
            if (badAngle) {
                appendLine(doc, "枪玉(" + u.x + ", " + u.y + ", " + u.r + ")角度可能需要改为2", suggest);
            }
        }
    }

    /** 检测愈玉/缮玉是否在单位列表中位置过于靠后。 */
    private void checkHealerRepairPosition(StyledDocument doc, SimpleAttributeSet suggest) {
        int healerIdx = -1;
        int repairIdx = -1;
        for (int i = 0; i < units.size(); i++) {
            Unit u = units.get(i);
            if (healerIdx < 0 && u.id == 18) healerIdx = i;
            if (repairIdx < 0 && u.id == 17) repairIdx = i;
            if (healerIdx >= 0 && repairIdx >= 0) break;
        }

        if (healerIdx >= 0) {
            for (int i = 0; i < healerIdx; i++) {
                Unit u = units.get(i);
                if (!u.isWallLike() && u.id != 18) {
                    Unit h = units.get(healerIdx);
                    appendLine(doc, "愈玉(" + h.x + ", " + h.y + ", " + h.r + ")可能需要前置", suggest);
                    break;
                }
            }
        }

        if (repairIdx >= 0) {
            for (int i = 0; i < repairIdx; i++) {
                Unit u = units.get(i);
                if (u.isWall()) {
                    Unit r = units.get(repairIdx);
                    appendLine(doc, "缮玉(" + r.x + ", " + r.y + ", " + r.r + ")可能需要前置", suggest);
                    break;
                }
            }
        }
    }

    // ============ 工具方法 ============

    private void appendLine(StyledDocument doc, String text, SimpleAttributeSet style) {
        try {
            if (doc.getLength() > 0) {
                doc.insertString(doc.getLength(), "\n", style);
            }
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException ignored) {}
    }

    private SimpleAttributeSet mkStyle(Color fg) {
        SimpleAttributeSet s = new SimpleAttributeSet();
        StyleConstants.setFontFamily(s, "黑体");
        StyleConstants.setFontSize(s, 14);
        StyleConstants.setForeground(s, fg != null ? fg : textPane.getForeground());
        return s;
    }
}
