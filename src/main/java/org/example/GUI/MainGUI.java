package org.example.GUI;

import org.example.Main;
import org.example.Setting;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;

public class MainGUI extends JFrame {

    private final TraceTab traceTab;
    private final CraftTab craftTab;
    private final UnitDexTab unitDexTab;
    private final LinkTab linkTab;
    private final ContributionTab contributionTab;

    public MainGUI() {
        setTitle("激突Kit v" + Main.VERSION);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1050, 720);
        setLocationRelativeTo(null);

        Image icon = loadIconImage("/icon.png");
        if (icon != null) {
            setIconImage(icon);
            addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowOpened(java.awt.event.WindowEvent e) {
                    setTaskbarIcon(icon);
                }
            });
        }

        // 菜单栏
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu(org.example.I18n.t("menu.options"));

        JCheckBoxMenuItem darkItem = new JCheckBoxMenuItem(org.example.I18n.t("menu.dark"), Main.DARK_MODE);
        darkItem.addActionListener(e -> toggleTheme(darkItem.isSelected()));
        fileMenu.add(darkItem);

        // 语言子菜单：各语言以本名显示
        JMenu langMenu = new JMenu(org.example.I18n.t("menu.language"));
        ButtonGroup langGroup = new ButtonGroup();
        addLangItem(langMenu, langGroup, "中文", org.example.I18n.ZH);
        addLangItem(langMenu, langGroup, "日本語", org.example.I18n.JA);
        addLangItem(langMenu, langGroup, "English", org.example.I18n.EN);
        fileMenu.add(langMenu);
        fileMenu.addSeparator();

        JMenuItem aboutItem = new JMenuItem(org.example.I18n.t("menu.about"));
        aboutItem.addActionListener(e -> {
            String msg = String.format(
                    "<html><div style='text-align:left;'>"
                    + "<b>激突Kit v%s</b><br><br>"
                    + "%s<br>"
                    + "XX<br>"
                    + "DeepSeek<br>"
                    + "15222HGH<br>"
                    + "MKTL"
                    + "</div></html>",
                    Main.VERSION,
                    org.example.I18n.t("about.author"));
            JOptionPane.showMessageDialog(this, msg, org.example.I18n.t("menu.about"), JOptionPane.INFORMATION_MESSAGE);
        });
        fileMenu.add(aboutItem);
        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem(org.example.I18n.t("menu.exit"));
        exitItem.addActionListener(e -> shutdown());
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        // 主标签页
        JTabbedPane mainTabs = new JTabbedPane();
        mainTabs.addTab(org.example.I18n.t("tab.battle"), new BattleTab());
        contributionTab = new ContributionTab();
        mainTabs.addTab(org.example.I18n.t("tab.contribution"), contributionTab);
        traceTab = new TraceTab(this);
        mainTabs.addTab(org.example.I18n.t("tab.trace"), traceTab);
        craftTab = new CraftTab(this);
        mainTabs.addTab(org.example.I18n.t("tab.craft"), craftTab);
        unitDexTab = new UnitDexTab(this);
        mainTabs.addTab(org.example.I18n.t("tab.dex"), unitDexTab);
        linkTab = new LinkTab();
        mainTabs.addTab(org.example.I18n.t("tab.links"), linkTab);

        // 主题色按钮浮动在右上角，不影响下方布局
        JPanel accentCircle = createAccentCircle();
        JLayeredPane layeredPane = new JLayeredPane();
        mainTabs.setBounds(0, 0, 1050, 720);
        accentCircle.setBounds(1050 - 28, 2, 24, 24);
        layeredPane.add(mainTabs, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(accentCircle, JLayeredPane.PALETTE_LAYER);
        layeredPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = layeredPane.getWidth();
                int h = layeredPane.getHeight();
                mainTabs.setBounds(0, 0, w, h);
                accentCircle.setBounds(w - 28, 2, 24, 24);
            }
        });
        add(layeredPane);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                shutdown();
            }
        });
    }

    /** 添加一个语言单选菜单项；点击后保存设置并重建主窗口使新语言生效。 */
    private void addLangItem(JMenu menu, ButtonGroup group, String label, String code) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(label);
        item.setSelected(code.equals(Main.LANGUAGE));
        item.addActionListener(e -> switchLanguage(code));
        group.add(item);
        menu.add(item);
    }

    private void switchLanguage(String code) {
        if (code.equals(Main.LANGUAGE)) {
            return;
        }
        Main.LANGUAGE = code;
        org.example.I18n.setLang(code);
        Setting.saveConfig();
        dispose();
        javax.swing.SwingUtilities.invokeLater(() -> new MainGUI().setVisible(true));
    }

    private static Image loadIconImage(String path) {
        try {
            java.net.URL url = MainGUI.class.getResource(path);
            return url != null ? ImageIO.read(url) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void setTaskbarIcon(Image icon) {
        try {
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                    taskbar.setIconImage(icon);
                }
            }
        } catch (Exception ignored) {}
    }

    private void toggleTheme(boolean dark) {
        try {
            Main.DARK_MODE = dark;
            Main.applyTheme(dark, Main.ACCENT_COLOR);
            Setting.saveConfig();
            updateDarkMode();
            com.formdev.flatlaf.FlatLaf.updateUI();
        } catch (Exception ignored) {}
    }

    private JPanel createAccentCircle() {
        JPanel circle = new JPanel() {
            {
                setOpaque(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                setToolTipText(org.example.I18n.t("tooltip.accent"));
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                try {
                    g2.setColor(Color.decode(Main.ACCENT_COLOR));
                } catch (Exception e) {
                    g2.setColor(new Color(0x26, 0x75, 0xBF));
                }
                g2.fillOval(2, 2, 20, 20);
                g2.setColor(Main.DARK_MODE ? Color.WHITE : Color.DARK_GRAY);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawOval(2, 2, 20, 20);
                g2.dispose();
            }
            @Override
            public Dimension getPreferredSize() { return new Dimension(24, 24); }
        };
        circle.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                Color current = null;
                try {
                    current = Color.decode(Main.ACCENT_COLOR);
                } catch (Exception ignored) {}
                Color newColor = ColorPicker.showDialog(MainGUI.this, current, Main.DARK_MODE, true);
                if (newColor != null) {
                    Main.ACCENT_COLOR = colorToHex(newColor);
                    Setting.saveConfig();
                    Main.applyTheme(Main.DARK_MODE, Main.ACCENT_COLOR);
                    updateDarkMode();
                    com.formdev.flatlaf.FlatLaf.updateUI();
                }
            }
        });
        return circle;
    }

    private static String colorToHex(Color c) {
        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    private void updateDarkMode() {
        if (contributionTab != null) {
            contributionTab.updateDarkMode();
        }
        if (traceTab != null) {
            traceTab.updateDarkMode();
        }
        if (craftTab != null) {
            craftTab.updateDarkMode();
        }
        if (unitDexTab != null) {
            unitDexTab.updateDarkMode();
        }
        if (linkTab != null) {
            linkTab.updateDarkMode();
        }
    }

    private void shutdown() {
        if (Main.pool != null && !Main.pool.isShutdown()) {
            Main.pool.shutdown();
        }
        dispose();
        System.exit(0);
    }
}
