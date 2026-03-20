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
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int maxWidth = Math.max(200, screenSize.width - 80);
        int maxHeight = Math.max(200, screenSize.height - 120);
        float scale = Math.min(1f, Math.min((float) maxWidth / width, (float) maxHeight / height));
        int windowWidth = Math.round(width * scale);
        int windowHeight = Math.round(height * scale);
        setSize(windowWidth, windowHeight);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 居中显示
        setResizable(false);

        canvas = new GameCanvas(width, height);
        add(canvas);

    }

    public void requestRender() {
        canvas.repaint();
    }

    public GameWindow setList(List<Shape> list){
        GameCanvas.shapeList = list;
        return this;
    }

    private static class GameCanvas extends JPanel {
        private static List<Shape> shapeList;
        private final int logicalWidth;
        private final int logicalHeight;
        private static final float CAMERA_OFFSET_Y = 60f;

        public GameCanvas(int logicalWidth, int logicalHeight) {
            setBackground(Color.BLACK);
            this.logicalWidth = logicalWidth;
            this.logicalHeight = logicalHeight;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            float scale = Math.min((float) w / logicalWidth, (float) h / logicalHeight);
            float drawW = logicalWidth * scale;
            float drawH = logicalHeight * scale;
            float offsetX = (w - drawW) / 2f;
            float offsetY = (h - drawH) / 2f + CAMERA_OFFSET_Y;
            g2d.setColor(Color.WHITE);
            g2d.drawString("frame: " + Main.time, 12, 18);
            g2d.drawString(Integer.toString(Main.hp[0]), 12, 32);
            g2d.drawString(Integer.toString(Main.hp[1]), 1200, 32);
            g2d.translate(offsetX, offsetY);
            g2d.scale(scale, scale);
            g2d.scale(0.96, 0.96);
            g2d.translate(-10, 320);
            for (Shape s : shapeList) {
                if (!Main.elements.containsValue(s)) {
                    continue;
                }
                s.draw(g2d);
            }
        }
    }
}