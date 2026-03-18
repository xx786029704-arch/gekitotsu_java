package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import org.example.Shape;

public class GameWindow extends JFrame {    //渲染窗口，全是AI写的和我没关系，反正最后也不用可视化
    private final GameCanvas canvas;

    public GameWindow(int width, int height, int targetFPS) {
        setTitle("Game Render Window");
        setSize(width, height);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 居中显示
        setResizable(false);

        canvas = new GameCanvas();
        add(canvas);

        int delay = 1000 / targetFPS;
        Timer renderTimer = new Timer(delay, e -> {
            canvas.repaint();
        });
        renderTimer.start();
    }

    private static class GameCanvas extends JPanel {
        public GameCanvas() {
            setBackground(Color.BLACK);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.WHITE);
            g2d.drawString("frame: " + Main.time, 12, 18);
            g2d.drawString(Integer.toString(Main.hp[0]), 12, 32);
            g2d.drawString(Integer.toString(Main.hp[1]), 1200, 32);
            g2d.scale(0.9, 0.9);
            g2d.translate(-10, 320);
            List<Shape> keys = new ArrayList<>(Main.elements.values());
            for (Shape s : keys) {
                s.draw(g2d);
            }
        }
    }
}