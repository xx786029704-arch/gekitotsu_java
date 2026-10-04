package org.example.GUI;

import org.example.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/** 阵型预览面板：绘制背景图 + 所有单位贴图，等比缩放适配容器。 */
public class FortPreviewPanel extends JPanel {

    // ---- 坐标映射 ----
    private static final double SCALE_X = 1.0;
    private static final double SCALE_Y = 1.0;

    // ---- 普通兵玉锚点（精灵内像素坐标，底边中心） ----
    private static final int anchor_x = 43;
    private static final int anchor_y = 55;

    // ---- 普通兵玉旋转后偏移 ----
    private static final int bias_x = 36;
    private static final int bias_y = 36;

    // ---- 核心锚点（精灵内像素坐标，中心） ----
    private static final int anchorCore_x = 60;
    private static final int anchorCore_y = 60;

    // ---- 核心偏移 ----
    private static final int biasCore_x = 72;
    private static final int biasCore_y = 74;

    private static final int SWATCH_SIZE = 18;
    private static final int SWATCH_MARGIN = 4;
    private static final Color DEFAULT_BG = new Color(0, 204, 255, 0);

    private List<Unit> units = List.of();
    private String name = "";
    private final BufferedImage bgImage;
    private final BufferedImage errorImage;
    private final boolean bgLoaded;
    private final Map<Integer, BufferedImage> unitImageCache = new HashMap<>();
    private Color bgColor = DEFAULT_BG;

    private final JButton saveButton;

    public FortPreviewPanel() {
        setOpaque(false);
        setLayout(null);
        bgImage = loadImage("/Bg/bg.png");
        errorImage = loadImage("/Bg/error.png");
        bgLoaded = bgImage != null;

        saveButton = new JButton(new ImageIcon(getClass().getResource("/save.png")));
        saveButton.setBorderPainted(false);
        saveButton.setContentAreaFilled(false);
        saveButton.setFocusPainted(false);
        saveButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveButton.setToolTipText("保存预览图");
        saveButton.addActionListener(e -> saveImage());
        add(saveButton);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                positionSaveButton();
            }
        });

        /*
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int sx = getWidth() - SWATCH_MARGIN - SWATCH_SIZE;
                int sy = SWATCH_MARGIN;
                if (e.getX() >= sx && e.getX() <= sx + SWATCH_SIZE
                        && e.getY() >= sy && e.getY() <= sy + SWATCH_SIZE) {
                    Window owner = SwingUtilities.getWindowAncestor(FortPreviewPanel.this);
                    Color newColor = ColorPicker.showDialog(owner, DEFAULT_BG, Main.DARK_MODE, false, true);
                    if (newColor != null) {
                        bgColor = newColor;
                        repaint();
                    }
                }
            }
        });
        */
    }

    private void positionSaveButton() {
        Dimension btnSize = saveButton.getPreferredSize();
        saveButton.setBounds(getWidth() - SWATCH_MARGIN - btnSize.width,
                getHeight() - SWATCH_MARGIN - btnSize.height,
                btnSize.width, btnSize.height);
    }

    private void saveImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File((!name.isEmpty() ? name : "fort_preview") + ".png"));
        chooser.setFileFilter(new FileNameExtensionFilter("PNG 图像 (*.png)", "png"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().contains(".")) {
            file = new File(file.getAbsolutePath() + ".png");
        }

        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        paintComponent(g2);
        g2.dispose();

        try {
            ImageIO.write(image, "png", file);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "保存失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void setFormation(String name, List<Unit> units) {
        this.units = units != null ? units : List.of();
        this.name = name;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int panelW = getWidth();
        int panelH = getHeight();
        if (panelW <= 0 || panelH <= 0) { g2.dispose(); return; }

        // 背景色填充
        g2.setColor(bgColor);
        g2.fillRect(0, 0, panelW, panelH);

        if (!bgLoaded || bgImage == null) {
            drawError(g2, panelW, panelH);
            //drawSwatch(g2);
            g2.dispose();
            return;
        }

        int bgW = bgImage.getWidth();
        int bgH = bgImage.getHeight();

        double scale = Math.min((double) panelW / bgW, (double) panelH / bgH);
        int drawW = (int) (bgW * scale);
        int drawH = (int) (bgH * scale);
        int offsetX = (panelW - drawW) / 2;
        int offsetY = (panelH - drawH) / 2;

        AffineTransform bgTx = AffineTransform.getTranslateInstance(offsetX, offsetY);
        bgTx.scale(scale, scale);
        g2.drawImage(bgImage, bgTx, null);

        // 先绘制类要塞壁单位（底层），再绘制非要塞壁单位（上层）
        for (Unit unit : units) {
            if (unit.isWallLike()) {
                drawUnit(g2, unit, scale, offsetX, offsetY);
            }
        }
        for (Unit unit : units) {
            if (!unit.isWallLike()) {
                drawUnit(g2, unit, scale, offsetX, offsetY);
            }
        }

        //drawSwatch(g2);
        g2.dispose();
    }

    private void drawUnit(Graphics2D g2, Unit unit, double scale, int offsetX, int offsetY) {
        int id = unit.id;

        BufferedImage sprite = unitImageCache.get(id);
        if (sprite == null) {
            sprite = loadImage(String.format("/Units/u%04d.png", id));
            if (sprite == null) return;
            unitImageCache.put(id, sprite);
        }

        double mapX = unit.x * SCALE_X;
        double mapY = unit.y * SCALE_Y;

        Graphics2D ug = (Graphics2D) g2.create();
        try {
            // 屏幕居中 + 缩放（后应用的变换写在前面）
            ug.translate(offsetX, offsetY);
            ug.scale(scale, scale);

            if (unit.isCore()) {
                // 核心：不旋转，锚点对准位置 + 偏移
                ug.translate(mapX - anchorCore_x + biasCore_x, mapY - anchorCore_y + biasCore_y);
            } else {
                // 非核心：先定位到 game 坐标 + 旋转后偏移
                ug.translate(mapX + bias_x, mapY + bias_y);
                // 旋转（围绕原点，即锚点位置）
                ug.rotate(Math.toRadians(unit.r));
                // 垂直翻转（90°~270°）
                if (unit.r >= 90 && unit.r <= 270) {
                    ug.scale(1, -1);
                }
                // 将精灵锚点移至原点
                ug.translate(-anchor_x, -anchor_y);
            }

            ug.drawImage(sprite, 0, 0, null);
        } finally {
            ug.dispose();
        }
    }

    private void drawError(Graphics2D g2, int panelW, int panelH) {
        if (errorImage != null) {
            int ew = errorImage.getWidth();
            int eh = errorImage.getHeight();
            int ex = (panelW - ew) / 2;
            int ey = (panelH - eh) / 2;
            g2.drawImage(errorImage, ex, ey, null);
        }
    }

    private void drawSwatch(Graphics2D g2) {
        int sx = getWidth() - SWATCH_MARGIN - SWATCH_SIZE;
        int sy = SWATCH_MARGIN;
        g2.setColor(bgColor);
        g2.fillRect(sx, sy, SWATCH_SIZE, SWATCH_SIZE);
        g2.setColor(org.example.Main.DARK_MODE ? new Color(200, 200, 200) : new Color(70, 70, 70));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(sx, sy, SWATCH_SIZE, SWATCH_SIZE);
    }

    private BufferedImage loadImage(String path) {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) return null;
            return ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }
}
