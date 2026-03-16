package org.example;

import javax.swing.*;
import java.awt.*;

public class GameWindow extends JFrame {
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

            for (Shape s : Main.elements) {
                s.draw(g2d);
            }
        }
    }
}