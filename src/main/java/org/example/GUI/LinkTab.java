package org.example.GUI;

import org.example.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class LinkTab extends JPanel {

    private record LinkEntry(String name, String url) {
    }

    private static final Object[][] CATEGORIES = {
        {"原作官网", new LinkEntry[]{
            new LinkEntry("激突要塞！公式サイト ", "https://suznooto.com/")
        }},
        {"国内社区", new LinkEntry[]{
            new LinkEntry("激突要塞百度贴吧 ", "https://tieba.baidu.com/f?kw=%E6%BF%80%E7%AA%81%E8%A6%81%E5%A1%9E"),
            new LinkEntry("激突驿站 ", "https://app-8uul3uungrup.appmiaoda.com/"),
            new LinkEntry("激突要塞！+萌娘百科 ", "https://moegirl.icu/%E6%BF%80%E7%AA%81%E8%A6%81%E5%A1%9E"),
            new LinkEntry("激突要塞比赛QQ群 ", "http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=2Ccfro01Pic3O3rsEyFY_W85u-EA6u68&authKey=2eJFmH%2B1lH7%2Bw%2BFWBkX2JAeEq5h4BA8MACVxtM9CW6NKjnhtAjcqkPMNdH76x20j&noverify=0&group_code=836444580")
        }},
        {"日本社区", new LinkEntry[]{
            new LinkEntry("Discord频道 ", "https://discord.com/channels/668116933471633428/896761917748703242"),
            new LinkEntry("青茶の要塞研究所 ", "https://bluechartfortress.web.fc2.com/tactics.html"),
            new LinkEntry("激突要塞！+wiki ", "https://wikiwiki.jp/gekitotsu/"),
            new LinkEntry("激突要塞！+攻略wiki ", "https://seesaawiki.jp/gekitotuwiki/"),
            new LinkEntry("激突要塞！+掲示板 ", "https://gekitotuchat.wiki.fc2.com/")
        }}
    };

    private final List<JLabel> urlLabels = new ArrayList<>();
    private Color fgColor, linkColor, urlColor;
    private Color linkHoverBg;
    private final JPanel contentPanel;

    public LinkTab() {
        setLayout(new BorderLayout());
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        buildContent();
        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                updateAllUrls();
            }
        });
        add(scroll);
        updateColors();
    }

    private void buildContent() {
        Font headerFont = new Font("黑体", Font.BOLD, 16);
        Font nameFont = new Font("黑体", Font.PLAIN, 14);
        Font urlFont = new Font("黑体", Font.PLAIN, 12);

        for (Object[] cat : CATEGORIES) {
            String title = (String) cat[0];
            LinkEntry[] links = (LinkEntry[]) cat[1];

            JLabel header = new JLabel(title);
            header.setFont(headerFont);
            header.setBorder(BorderFactory.createEmptyBorder(12, 0, 8, 0));
            contentPanel.add(header);

            for (LinkEntry link : links) {
                JPanel linkPanel = createLinkPanel(link, nameFont, urlFont);
                contentPanel.add(linkPanel);
                contentPanel.add(Box.createVerticalStrut(4));
            }
        }
    }

    private JPanel createLinkPanel(LinkEntry link, Font nameFont, Font urlFont) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 0));

        JLabel nameLabel = new JLabel(link.name);
        nameLabel.setFont(nameFont);
        panel.add(nameLabel);

        JLabel urlLabel = new JLabel();
        urlLabel.setFont(urlFont);
        urlLabel.putClientProperty("fullUrl", link.url);
        panel.add(urlLabel);
        urlLabels.add(urlLabel);

        nameLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        urlLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter linkHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(link.url));
                } catch (Exception ignored) {}
            }
            @Override
            public void mouseEntered(MouseEvent e) {
                if (linkHoverBg != null) {
                    panel.setOpaque(true);
                    panel.setBackground(linkHoverBg);
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                panel.setOpaque(false);
                panel.setBackground(null);
            }
        };
        nameLabel.addMouseListener(linkHandler);
        urlLabel.addMouseListener(linkHandler);

        return panel;
    }

    private void updateAllUrls() {
        for (JLabel label : urlLabels) {
            String fullUrl = (String) label.getClientProperty("fullUrl");
            if (fullUrl != null && label.getWidth() > 0) {
                label.setText(truncateWithEllipsis(fullUrl, label.getFontMetrics(label.getFont()), label.getWidth()));
            }
        }
    }

    private static String truncateWithEllipsis(String text, FontMetrics fm, int maxWidth) {
        if (maxWidth <= 0 || fm.stringWidth(text) <= maxWidth) return text;
        String ellipsis = "...";
        int dotW = fm.stringWidth(ellipsis);
        for (int i = text.length() - 1; i > 0; i--) {
            if (fm.stringWidth(text.substring(0, i)) + dotW <= maxWidth) {
                return text.substring(0, i) + ellipsis;
            }
        }
        return ellipsis;
    }

    private void updateColors() {
        if (Main.DARK_MODE) {
            fgColor = new Color(0xE0E0E0);
            linkColor = new Color(0x6DB3F2);
            urlColor = new Color(0x999999);
            linkHoverBg = new Color(0x333333);
            contentPanel.setBackground(new Color(0x2B2B2B));
            setBackground(new Color(0x2B2B2B));
        } else {
            fgColor = Color.BLACK;
            linkColor = new Color(0x1A6DB5);
            urlColor = new Color(0x888888);
            linkHoverBg = new Color(0xF0F0F0);
            contentPanel.setBackground(Color.WHITE);
            setBackground(Color.WHITE);
        }
        applyColorsToTree(contentPanel);
    }

    private void applyColorsToTree(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JLabel label) {
                String fullUrl = (String) label.getClientProperty("fullUrl");
                if (fullUrl != null) {
                    label.setForeground(urlColor);
                } else {
                    Font f = label.getFont();
                    if (f != null && f.getStyle() == Font.BOLD) {
                        label.setForeground(fgColor);
                    } else {
                        label.setForeground(linkColor);
                    }
                }
            }
            if (comp instanceof Container) {
                applyColorsToTree((Container) comp);
            }
        }
    }

    public void updateDarkMode() {
        updateColors();
    }
}
