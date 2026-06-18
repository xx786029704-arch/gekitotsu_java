package org.example.GUI;

import org.example.Main;

import javax.swing.*;
import java.awt.*;

/** 可绘制落点指示线的工作台面板。 */
public class WorkflowDropPanel extends JPanel {
    int dropLineY = -1;

    public WorkflowDropPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(true);
    }

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
            g2.drawLine(0, dropLineY - 2, getWidth() - 8, dropLineY - 2);
            g2.fillOval(-2, dropLineY - 5, 5, 5);
            g2.dispose();
        }
    }
}
