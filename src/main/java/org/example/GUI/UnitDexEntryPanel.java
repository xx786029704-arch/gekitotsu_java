package org.example.GUI;

import org.example.Main;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

/** 单位图鉴左侧列表条目：缩略图 + 名称，支持点击选中。 */
public class UnitDexEntryPanel extends JPanel {
    private final int unitId;
    private boolean selected;
    private final ThumbnailPanel thumbnail;
    private static final int THUMB_SIZE = 32;

    public UnitDexEntryPanel(int unitId, Consumer<Integer> onSelect) {
        super(new BorderLayout(8, 0));
        this.unitId = unitId;

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Main.DARK_MODE ? new Color(80, 80, 80) : new Color(200, 200, 200), 1),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        thumbnail = new ThumbnailPanel(unitId);
        thumbnail.setPreferredSize(new Dimension(THUMB_SIZE, THUMB_SIZE));
        add(thumbnail, BorderLayout.WEST);

        JLabel nameLabel = new JLabel(Unit.infos[unitId].name());
        nameLabel.setFont(new Font("黑体", Font.PLAIN, 13));
        add(nameLabel, BorderLayout.CENTER);

        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                onSelect.accept(unitId);
            }
        });
    }

    public int getUnitId() { return unitId; }

    public void setSelected(boolean s) {
        if (selected == s) return;
        selected = s;
        if (s) {
            Color lineColor;
            try {
                lineColor = Color.decode(Main.ACCENT_COLOR);
            } catch (Exception e) {
                lineColor = new Color(0x26, 0x75, 0xBF);
            }
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(lineColor, 2),
                    BorderFactory.createEmptyBorder(4, 7, 4, 7)));
        } else {
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Main.DARK_MODE ? new Color(80, 80, 80) : new Color(200, 200, 200), 1),
                    BorderFactory.createEmptyBorder(5, 8, 5, 8)));
        }
    }

    /** 绘制缩略图的小面板，将单位贴图缩放到 28x28 居中显示。 */
    private static class ThumbnailPanel extends JPanel {
        private BufferedImage thumb;

        ThumbnailPanel(int unitId) {
            setOpaque(false);
            thumb = loadThumb(unitId);
        }

        private static BufferedImage loadThumb(int id) {
            String path = String.format("/Units/u%04d.png", id);
            try (InputStream in = ThumbnailPanel.class.getResourceAsStream(path)) {
                if (in == null) return null;
                BufferedImage original = ImageIO.read(in);
                if (original == null) return null;

                double scale = Unit.isCore(id) ? 0.3 : 0.6;
                int w = (int)(original.getWidth() * scale);
                int h = (int)(original.getHeight() * scale);
                if (w < 1) w = 1;
                if (h < 1) h = 1;

                Image scaled = original.getScaledInstance(w, h, Image.SCALE_SMOOTH);
                BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = result.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int x = Unit.isCore(id) ? 0 : 8;
                int y = Unit.isCore(id) ? 0 : -4;
                g2.drawImage(scaled, x, y, null);
                g2.dispose();
                return result;
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (thumb == null) return;
            int tw = thumb.getWidth();
            int th = thumb.getHeight();
            int x = (getWidth() - tw) / 2;
            int y = (getHeight() - th) / 2;
            g.drawImage(thumb, x, y, null);
        }
    }
}
