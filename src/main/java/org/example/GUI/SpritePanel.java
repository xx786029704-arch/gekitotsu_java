package org.example.GUI;

import org.example.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

/** 单位贴图绘制面板。贴图锚点对齐到面板内固定位置，不旋转。 */
public class SpritePanel extends JPanel {

    public static double coreSpriteScale = 0.5;
    public static double nonCoreSpriteScale = 1.0;

    private static final int CORE_ANCHOR_X = 60;
    private static final int CORE_ANCHOR_Y = 60;
    private static final int UNIT_ANCHOR_X = 43;
    private static final int UNIT_ANCHOR_Y = 55;

    private BufferedImage sprite;
    private boolean isCore;

    public SpritePanel() {
        setOpaque(true);
        setBorder(BorderFactory.createLineBorder(
                Main.DARK_MODE ? new Color(100, 100, 100) : new Color(180, 180, 180), 1));
    }

    public void updateDarkMode() {
        setBorder(BorderFactory.createLineBorder(
                Main.DARK_MODE ? new Color(100, 100, 100) : new Color(180, 180, 180), 1));
    }

    public void setUnit(Unit unit) {
        if (unit == null) {
            sprite = null;
            repaint();
            return;
        }
        isCore = unit.isCore();
        sprite = loadSprite(unit.id);
        repaint();
    }

    private BufferedImage loadSprite(int id) {
        String path = String.format("/Units/u%04d.png", id);
        try (java.io.InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) return null;
            return ImageIO.read(in);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (sprite == null) return;
        int w = getWidth();
        int h = getHeight();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.clipRect(0, 0, w, h);

        double scale = isCore ? coreSpriteScale : nonCoreSpriteScale;
        int anchorX = isCore ? CORE_ANCHOR_X : UNIT_ANCHOR_X;
        int anchorY = isCore ? CORE_ANCHOR_Y : UNIT_ANCHOR_Y;

        int drawX = (int) (w * 0.5 - anchorX * scale);
        int drawY = (int) (h * 0.5 - anchorY * scale);
        int drawW = (int) (sprite.getWidth() * scale);
        int drawH = (int) (sprite.getHeight() * scale);

        g2.drawImage(sprite, drawX, drawY, drawW, drawH, null);
        g2.dispose();
    }
}
