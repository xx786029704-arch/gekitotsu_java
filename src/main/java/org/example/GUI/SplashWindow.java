package org.example.GUI;

import org.example.Main;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * 启动开屏窗口：在公式表与主界面加载期间立即显示，并播放加载动画。
 * 无边框半透明 JWindow：圆角卡片 + 软件图标 + 状态文字 + 循环进度条。
 * 由 {@code Main.main()} 在 EDT 上创建；主窗口打开后淡出关闭。
 */
public class SplashWindow extends JWindow {
    private static final int WIDTH = 440;
    private static final int HEIGHT = 212;
    private static final int INSET = 10;
    private static final int ARC = 18;

    private BufferedImage icon;
    private String status = org.example.I18n.t("splash.starting");
    private double phase = 0;
    private long shownAt = 0;
    private final Timer animTimer;
    private static final String[] msgs = {
            "23333333…………",
            "怎么弄的笑脸老家在哪弄的",
            "神天马问65什么是茄子",
            "我先用加速，你就被我撞的找不到东南西北了",
            "我再用梱玉，你几乎毫无胜算",
            "无垠宇宙中那些自不量力的事物",
            "终将被混沌所化为尘埃",
            "纳尼，历史被改写了",
            "轻骑利剑，吾必当先",
            "内心的情感爆发，乱作一团",
            "不是周冠军，而是 总 冠 军",
            "不用，我觉得我的学员很棒",
            "你是逆天对吗",
            "此阵法无敌",
            "无殇好虾头",
            "不穿裤子先穿鞋，不当孙子先当爷，爬",
            "战",
            "时来天地皆同战",
            "俄顷重症肌无力",
            "又搞这个难出来给我",
            "爷必出线",
            "吾逐渐明白，不是那个时代只有19在玩激突",
            "正如404说的，不想怎么不茄，想怎么茄不被发现你是"
    };
    private static final String msg = msgs[(int) (Math.random()*msgs.length)];

    private SplashWindow() {
        setSize(WIDTH, HEIGHT);
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        setFocusableWindowState(false);
        enableTranslucency();
        try {
            icon = ImageIO.read(SplashWindow.class.getResource("/icon.png"));
        } catch (Exception ignored) {
        }

        JPanel content = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                paintCard(g);
            }
        };
        content.setOpaque(false);
        setContentPane(content);

        animTimer = new Timer(25, e -> {
            phase += 0.012;
            if (phase >= 1) phase -= 1;
            repaint();
        });
        animTimer.setCoalesce(true);
    }

    /** 在 EDT 上立即创建并显示开屏窗口；创建失败时返回 null。 */
    public static SplashWindow createAndShow() {
        final SplashWindow[] ref = new SplashWindow[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    SplashWindow w = new SplashWindow();
                    w.setVisible(true);
                    w.shownAt = System.currentTimeMillis();
                    w.fadeIn();
                    w.animTimer.start();
                    ref[0] = w;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ref[0];
    }

    /** 更新状态文字（线程安全）。 */
    public void setStatus(String text) {
        SwingUtilities.invokeLater(() -> {
            status = text;
            repaint();
        });
    }

    /** 淡出并关闭窗口；若显示过短则先补足最短展示时间，避免一闪而过。 */
    public void close() {
        SwingUtilities.invokeLater(() -> {
            long elapsed = System.currentTimeMillis() - shownAt;
            if (elapsed >= 450) {
                fadeOutAndDispose();
                return;
            }
            Timer t = new Timer((int) (450 - elapsed), e -> fadeOutAndDispose());
            t.setRepeats(false);
            t.start();
        });
    }

    /** 立即关闭窗口（异常兜底）。 */
    public void closeImmediately() {
        SwingUtilities.invokeLater(this::dispose);
    }

    private void enableTranslucency() {
        Color opaqueBg = new Color(0x22, 0x24, 0x2A);
        try {
            boolean supported = GraphicsEnvironment.getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
            setBackground(supported ? new Color(0, 0, 0, 0) : opaqueBg);
        } catch (Exception e) {
            setBackground(opaqueBg);
        }
    }

    private void fadeIn() {
        try {
            setOpacity(0f);
            final int steps = 4;
            final int[] i = {0};
            Timer t = new Timer(16, null);
            t.addActionListener(e -> {
                i[0]++;
                setOpacity(Math.min(1f, i[0] / (float) steps));
                if (i[0] >= steps) t.stop();
            });
            t.start();
        } catch (Exception ignored) {
        }
    }

    private void fadeOutAndDispose() {
        if (!isDisplayable()) return;
        animTimer.stop();
        try {
            final int steps = 4;
            final int[] i = {0};
            Timer t = new Timer(16, null);
            t.addActionListener(e -> {
                i[0]++;
                setOpacity(Math.max(0f, 1f - i[0] / (float) steps));
                if (i[0] >= steps) {
                    t.stop();
                    dispose();
                }
            });
            t.start();
        } catch (Exception e) {
            dispose();
        }
    }

    @Override
    public void dispose() {
        animTimer.stop();
        super.dispose();
    }

    // --- 绘制 ---

    private Color accent() {
        try {
            return Color.decode(Main.ACCENT_COLOR);
        } catch (Exception e) {
            return new Color(0x26, 0x75, 0xBF);
        }
    }

    private void paintCard(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int w = getWidth(), h = getHeight();
        boolean dark = Main.DARK_MODE;
        Color accent = accent();

        // 投影
        g2.setColor(new Color(0, 0, 0, 14));
        for (int i = 6; i >= 1; i--) {
            g2.fill(new RoundRectangle2D.Double(INSET - i, INSET - i + 1,
                    w - 2.0 * (INSET - i), h - 2.0 * (INSET - i), ARC + i, ARC + i));
        }

        // 卡片背景
        Color top = dark ? new Color(0x2C, 0x2F, 0x37) : new Color(0xFF, 0xFF, 0xFF);
        Color bottom = dark ? new Color(0x21, 0x23, 0x29) : new Color(0xF2, 0xF4, 0xF7);
        RoundRectangle2D card = new RoundRectangle2D.Double(INSET, INSET,
                w - 2.0 * INSET, h - 2.0 * INSET, ARC, ARC);
        g2.setPaint(new GradientPaint(0, INSET, top, 0, h - INSET, bottom));
        g2.fill(card);

        // 顶部主题色高光条（裁剪进圆角）
        Shape clip = g2.getClip();
        g2.clip(card);
        g2.setColor(accent);
        g2.fillRect(INSET, INSET, w - 2 * INSET, 4);
        g2.setClip(clip);

        // 卡片描边
        g2.setColor(dark ? new Color(0x3D, 0x41, 0x4A) : new Color(0xD8, 0xDC, 0xE2));
        g2.draw(new RoundRectangle2D.Double(INSET + 0.5, INSET + 0.5,
                w - 2.0 * INSET - 1, h - 2.0 * INSET - 1, ARC, ARC));

        // 图标
        if (icon != null) {
            int size = 92;
            int iconX = INSET + 22;
            int iconY = INSET + 28;
            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 26));
            g2.fillOval(iconX + size / 2 - 56, iconY + size / 2 - 56, 112, 112);
            g2.drawImage(icon, iconX, iconY, size, size, null);
        }

        // 标题与版本
        int textX = 152;
        g2.setFont(org.example.I18n.font(Font.BOLD, 27));
        g2.setColor(dark ? new Color(0xF2, 0xF3, 0xF5) : new Color(0x20, 0x24, 0x2B));
        g2.drawString("激突Kit", textX, 66);

        String ver = "v" + Main.VERSION;
        Font verFont = org.example.I18n.font(Font.BOLD, 12);
        g2.setFont(verFont);
        FontMetrics vfm = g2.getFontMetrics();
        int chipW = vfm.stringWidth(ver) + 18;
        int chipH = 20;
        int chipY = 78;
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), dark ? 46 : 34));
        g2.fillRoundRect(textX, chipY, chipW, chipH, chipH, chipH);
        g2.setColor(accent);
        g2.drawString(ver, textX + 9, chipY + 15);

        // 副标题
        g2.setFont(org.example.I18n.font(Font.PLAIN, 12));
        g2.setColor(dark ? new Color(0x9A, 0xA0, 0xAA) : new Color(0x7A, 0x80, 0x89));
        g2.drawString(msg, textX + 1, 118);

        // 分隔线
        g2.setColor(dark ? new Color(0x3A, 0x3E, 0x47) : new Color(0xE1, 0xE4, 0xE9));
        g2.drawLine(INSET + 20, 144, w - INSET - 20, 144);

        // 状态文字
        g2.setFont(org.example.I18n.font(Font.PLAIN, 12));
        g2.setColor(dark ? new Color(0xB8, 0xBC, 0xC4) : new Color(0x5A, 0x5F, 0x68));
        g2.drawString(status, INSET + 20, 168);

        // 进度条
        int barX = INSET + 20;
        int barY = 184;
        int barW = w - 2 * INSET - 40;
        int barH = 5;
        RoundRectangle2D track = new RoundRectangle2D.Double(barX, barY, barW, barH, barH, barH);
        g2.setColor(dark ? new Color(0x38, 0x3C, 0x45) : new Color(0xE4, 0xE7, 0xEC));
        g2.fill(track);
        Shape oldClip = g2.getClip();
        g2.clip(track);
        int segW = Math.max(70, barW / 3);
        int segX = (int) (phase * (barW + segW)) - segW;
        g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 235));
        g2.fillRoundRect(barX + segX, barY, segW, barH, barH, barH);
        g2.setClip(oldClip);

        g2.dispose();
    }
}
