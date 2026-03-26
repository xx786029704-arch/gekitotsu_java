package org.example;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import org.example.Shape;
import org.example.elements.Ball;
import org.example.elements.Wall;

public class GameWindow extends JFrame {    //渲染窗口，全是AI写的和我没关系，反正最后也不用可视化
    private final GameCanvas canvas;
    protected final Game game;

    public GameWindow(Game game, int width, int height, int targetFPS) {
        this.game = game;
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

        canvas = new GameCanvas(game, width, height);
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
        protected final Game game;

        public GameCanvas(Game game, int logicalWidth, int logicalHeight) {
            setBackground(Color.BLACK);
            this.game = game;
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
            g2d.drawString("frame: " + this.game.time, 12, 18);
            g2d.drawString(Integer.toString(this.game.hp[0]), 12, 32);
            g2d.drawString(Integer.toString(this.game.hp[1]), 1200, 32);
            g2d.translate(offsetX, offsetY);
            g2d.scale(scale, scale);
            g2d.scale(0.96, 0.96);
            g2d.translate(39, 320);
            g2d.setColor(Color.gray);
            g2d.drawLine(0,-600,0,582);
            g2d.drawLine(1920,-600,1920,582);
            g2d.drawLine(0,582,1920,582);
            g2d.setColor(Color.WHITE);
            for (Shape s : shapeList) {
                if (!this.game.elements.containsValue(s)) {
                    continue;
                }
                s.draw(g2d);
                if (Settings.SHOW_UNIT_HP && s instanceof Ball) {
                    Ball unit = (Ball) s;
                    Color prev = g2d.getColor();
                    g2d.setColor(Color.GREEN);
                    g2d.drawString(Integer.toString(unit.hp), (int) unit.x - 4, (int) unit.y + 4);
                    g2d.setColor(prev);
                } else if (Settings.SHOW_UNIT_HP && s instanceof Wall) {
                    Wall wall = (Wall) s;
                    Color prev = g2d.getColor();
                    g2d.setColor(Color.CYAN);
                    g2d.drawString(Integer.toString(wall.hp), (int) wall.x - 4, (int) wall.y + 4);
                    g2d.setColor(prev);
                }
            }
        }
    }
}
