package org.example.GUI;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/** 通用颜色选择器，供主题色和轨迹色复用。 */
public class ColorPicker extends JDialog {

    private final Color originalColor;
    private float hue, saturation, brightness;
    private int alpha = 255;
    private final boolean isDarkMode;
    private final boolean showReset;
    private final boolean supportAlpha;
    private boolean adjusting;

    private Color result;

    private SBPanel sbPanel;
    private HuePanel huePanel;
    private JSpinner rSpinner, gSpinner, bSpinner, aSpinner;
    private SwatchPanel currentSwatch;

    /** 打开模态取色器，返回选中颜色；取消返回 null。 */
    public static Color showDialog(Component parent, Color initial, boolean darkMode, boolean showReset) {
        return showDialog(parent, initial, darkMode, showReset, false);
    }

    /** 打开模态取色器（可指定是否支持 alpha 通道）。 */
    public static Color showDialog(Component parent, Color initial, boolean darkMode, boolean showReset, boolean supportAlpha) {
        Window owner = parent != null ? SwingUtilities.getWindowAncestor(parent) : null;
        Color init = initial != null ? initial : Color.WHITE;
        ColorPicker cp = new ColorPicker(owner, init, darkMode, showReset, supportAlpha);
        cp.setVisible(true);
        return cp.result;
    }

    private ColorPicker(Window owner, Color initial, boolean darkMode, boolean showReset, boolean supportAlpha) {
        super(owner, "颜色选择器", ModalityType.APPLICATION_MODAL);
        this.originalColor = initial;
        this.isDarkMode = darkMode;
        this.showReset = showReset;
        this.supportAlpha = supportAlpha;

        float[] hsb = Color.RGBtoHSB(initial.getRed(), initial.getGreen(), initial.getBlue(), null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
        if (supportAlpha) {
            this.alpha = initial.getAlpha();
        }

        buildUI();
        setResizable(false);
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        JPanel content = new JPanel(new BorderLayout(12, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ---- 取色主行 ----
        JPanel pickerRow = new JPanel();
        pickerRow.setLayout(new BoxLayout(pickerRow, BoxLayout.X_AXIS));

        sbPanel = new SBPanel();
        pickerRow.add(sbPanel);
        pickerRow.add(Box.createHorizontalStrut(8));

        huePanel = new HuePanel();
        pickerRow.add(huePanel);
        pickerRow.add(Box.createHorizontalStrut(12));

        JPanel rightPanel = buildRightPanel();
        pickerRow.add(rightPanel);

        content.add(pickerRow, BorderLayout.CENTER);

        // ---- 底部：按钮 ----
        Font labelFont = new Font("黑体", Font.PLAIN, 12);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        
        JButton cancelBtn = new JButton("取消");
        cancelBtn.setFont(labelFont);
        cancelBtn.addActionListener(e -> {
            result = null;
            dispose();
        });
        buttonRow.add(cancelBtn);
        content.add(buttonRow, BorderLayout.SOUTH);

        JButton okBtn = new JButton("确定");
        okBtn.setFont(labelFont);
        okBtn.addActionListener(e -> {
            result = getCurrentColor();
            dispose();
        });
        buttonRow.add(okBtn);

        // RGB 监听（双向同步防递归）
        ChangeListener rgbListener = e -> {
            if (adjusting) return;
            int r = (int) rSpinner.getValue();
            int g = (int) gSpinner.getValue();
            int b = (int) bSpinner.getValue();
            float[] hsb = Color.RGBtoHSB(r, g, b, null);
            hue = hsb[0];
            saturation = hsb[1];
            brightness = hsb[2];
            sbPanel.rebuildSpectrum();
            sbPanel.repaint();
            huePanel.repaint();
            currentSwatch.repaint();
        };
        rSpinner.addChangeListener(rgbListener);
        gSpinner.addChangeListener(rgbListener);
        bSpinner.addChangeListener(rgbListener);

        setContentPane(content);
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));

        // 当前色预览
        currentSwatch = new SwatchPanel(this::getCurrentColor);
        currentSwatch.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(currentSwatch);
        panel.add(Box.createVerticalStrut(8));

        // RGB 竖排
        Font labelFont = new Font("黑体", Font.PLAIN, 12);
        Color labelFg = isDarkMode ? Color.WHITE : Color.BLACK;

        JPanel rgbPanel = new JPanel();
        rgbPanel.setLayout(new BoxLayout(rgbPanel, BoxLayout.Y_AXIS));
        rgbPanel.setAlignmentX(LEFT_ALIGNMENT);
        rgbPanel.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));

        rgbPanel.add(createRgbRow("R:", rSpinner = new JSpinner(new SpinnerNumberModel(initialRed(), 0, 255, 1)), labelFont, labelFg));
        rgbPanel.add(Box.createVerticalStrut(2));
        rgbPanel.add(createRgbRow("G:", gSpinner = new JSpinner(new SpinnerNumberModel(initialGreen(), 0, 255, 1)), labelFont, labelFg));
        rgbPanel.add(Box.createVerticalStrut(2));
        rgbPanel.add(createRgbRow("B:", bSpinner = new JSpinner(new SpinnerNumberModel(initialBlue(), 0, 255, 1)), labelFont, labelFg));

        if (supportAlpha) {
            rgbPanel.add(Box.createVerticalStrut(2));
            rgbPanel.add(createRgbRow("A:", aSpinner = new JSpinner(new SpinnerNumberModel(alpha, 0, 255, 1)), labelFont, labelFg));
            aSpinner.addChangeListener(e -> {
                if (adjusting) return;
                alpha = (int) aSpinner.getValue();
                currentSwatch.repaint();
            });
        }

        panel.add(rgbPanel);

        // 重置按钮（可选）
        if (showReset) {
            panel.add(Box.createVerticalStrut(8));
            JButton resetBtn = createResetButton();
            resetBtn.setAlignmentX(LEFT_ALIGNMENT);
            panel.add(resetBtn);
        }

        return panel;
    }

    private JPanel createRgbRow(String label, JSpinner spinner, Font font, Color fg) {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setMaximumSize(new Dimension(100, 24));
        JLabel lbl = new JLabel(label);
        lbl.setFont(font);
        lbl.setForeground(fg);
        lbl.setPreferredSize(new Dimension(16, 20));
        row.add(lbl, BorderLayout.WEST);
        row.add(spinner, BorderLayout.CENTER);
        return row;
    }

    private int initialRed()   { return originalColor.getRed(); }
    private int initialGreen() { return originalColor.getGreen(); }
    private int initialBlue()  { return originalColor.getBlue(); }

    private Color getCurrentColor() {
        int rgb = Color.HSBtoRGB(hue, saturation, brightness);
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        if (supportAlpha) {
            return new Color(r, g, b, alpha);
        }
        return new Color(r, g, b);
    }

    /** 由 SB/Hue 面板调用，同步更新 RGB 和当前色预览。 */
    private void updateFromHSB() {
        adjusting = true;
        Color c = getCurrentColor();
        rSpinner.setValue(c.getRed());
        gSpinner.setValue(c.getGreen());
        bSpinner.setValue(c.getBlue());
        if (supportAlpha) {
            aSpinner.setValue(alpha);
        }
        adjusting = false;
        huePanel.repaint();
        currentSwatch.repaint();
    }

    private JButton createResetButton() {
        ImageIcon icon = loadResetIcon();
        JButton btn;
        if (icon != null) {
            btn = new JButton(icon);
        } else {
            btn = new JButton("重置");
        }
        btn.setFont(new Font("黑体", Font.PLAIN, 12));
        btn.setToolTipText("恢复默认颜色");
        btn.addActionListener(e -> {
            Color defaultColor = new Color(0x26, 0x75, 0xBF);
            float[] hsb = Color.RGBtoHSB(
                    defaultColor.getRed(), defaultColor.getGreen(), defaultColor.getBlue(), null);
            hue = hsb[0];
            saturation = hsb[1];
            brightness = hsb[2];
            sbPanel.rebuildSpectrum();
            sbPanel.repaint();
            huePanel.repaint();
            updateFromHSB();
        });
        return btn;
    }

    private ImageIcon loadResetIcon() {
        try {
            BufferedImage img = ImageIO.read(new File("assets/reset.png"));
            if (img == null) return null;
            if (isDarkMode) {
                img = invertImage(img);
            }
            return new ImageIcon(img);
        } catch (IOException e) {
            return null;
        }
    }

    private static BufferedImage invertImage(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int argb = src.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = 255 - ((argb >> 16) & 0xFF);
                int g = 255 - ((argb >> 8) & 0xFF);
                int b = 255 - (argb & 0xFF);
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    // ==================== SB 光谱面板 ====================

    private class SBPanel extends JPanel {
        private BufferedImage spectrumImage;
        private static final int SIZE = 256;

        SBPanel() {
            setPreferredSize(new Dimension(SIZE, SIZE));
            setMinimumSize(new Dimension(150, 150));
            rebuildSpectrum();

            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e)  { updateSB(e); }
                @Override public void mouseDragged(MouseEvent e)  { updateSB(e); }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        void rebuildSpectrum() {
            if (spectrumImage == null) {
                spectrumImage = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
            }
            for (int x = 0; x < SIZE; x++) {
                float s = (float) x / SIZE;
                for (int y = 0; y < SIZE; y++) {
                    float b = 1f - (float) y / SIZE;
                    int rgb = Color.HSBtoRGB(hue, s, b);
                    spectrumImage.setRGB(x, y, rgb);
                }
            }
        }

        private void updateSB(MouseEvent e) {
            int w = getWidth();
            int h = getHeight();
            if (w == 0 || h == 0) return;
            saturation = Math.max(0, Math.min(1, (float) e.getX() / w));
            brightness = Math.max(0, Math.min(1, 1f - (float) e.getY() / h));
            updateFromHSB();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(spectrumImage, 0, 0, getWidth(), getHeight(), null);

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = (int) (saturation * getWidth());
            int cy = (int) ((1f - brightness) * getHeight());
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(Color.WHITE);
            g2.drawOval(cx - 6, cy - 6, 12, 12);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(Color.BLACK);
            g2.drawOval(cx - 5, cy - 5, 10, 10);
            g2.dispose();
        }
    }

    // ==================== 色相滑杆 ====================

    private class HuePanel extends JPanel {
        private final BufferedImage hueImage;
        private static final int WIDTH = 24;
        private static final int HEIGHT = 256;

        HuePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setMinimumSize(new Dimension(18, 150));
            hueImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < HEIGHT; y++) {
                int rgb = Color.HSBtoRGB((float) y / HEIGHT, 1f, 1f);
                for (int x = 0; x < WIDTH; x++) {
                    hueImage.setRGB(x, y, rgb);
                }
            }

            MouseAdapter ma = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e)  { updateHue(e); }
                @Override public void mouseDragged(MouseEvent e)  { updateHue(e); }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private void updateHue(MouseEvent e) {
            int h = getHeight();
            if (h == 0) return;
            hue = Math.max(0, Math.min(1, (float) e.getY() / h));
            sbPanel.rebuildSpectrum();
            sbPanel.repaint();
            updateFromHSB();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(hueImage, 0, 0, getWidth(), getHeight(), null);

            int hy = (int) (hue * getHeight());
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(Color.WHITE);
            g2.drawLine(0, hy, getWidth(), hy);
            g2.dispose();
        }
    }

    // ==================== 颜色预览色块 ====================

    private class SwatchPanel extends JPanel {
        private final java.util.function.Supplier<Color> colorSupplier;
        private BufferedImage checkerCache;
        private int cacheW, cacheH;

        SwatchPanel(java.util.function.Supplier<Color> supplier) {
            this.colorSupplier = supplier;
            setPreferredSize(new Dimension(50, 26));
            setMinimumSize(new Dimension(50, 26));
            setMaximumSize(new Dimension(50, 26));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth();
            int h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int swX = 2, swY = 2, swW = w - 4, swH = h - 4;
            Color c = colorSupplier.get();

            if (supportAlpha) {
                // 棋盘格显示透明
                drawChecker(g2, swX, swY, swW, swH);
            }

            g2.setColor(c);
            g2.fillRect(swX, swY, swW, swH);
            g2.setColor(isDarkMode ? Color.WHITE : Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(swX, swY, swW, swH);
            g2.dispose();
        }

        private void drawChecker(Graphics2D g2, int x, int y, int w, int h) {
            int size = 6;
            if (checkerCache == null || cacheW != w || cacheH != h) {
                checkerCache = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D cg = checkerCache.createGraphics();
                for (int cy = 0; cy < h; cy += size) {
                    for (int cx = 0; cx < w; cx += size) {
                        boolean even = ((cx / size) + (cy / size)) % 2 == 0;
                        cg.setColor(even ? Color.LIGHT_GRAY : Color.WHITE);
                        cg.fillRect(cx, cy, size, size);
                    }
                }
                cg.dispose();
                cacheW = w;
                cacheH = h;
            }
            g2.drawImage(checkerCache, x, y, null);
        }
    }
}
