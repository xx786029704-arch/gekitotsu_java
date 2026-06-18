package org.example.GUI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.Main;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/** 单位图鉴标签页：左侧单位列表（1/5）+ 右侧详情（4/5）。 */
public class UnitDexTab extends JPanel {

    private final JScrollPane listScroll;
    private final JPanel listContent;
    private final UnitDexDetailPanel detailPanel;
    private final JScrollPane detailScroll;

    private int selectedUnitId = -1;
    private JsonNode unitDetailJson;

    private static final int UNIT_COUNT = 63; // id 0~62

    public UnitDexTab(JFrame parentFrame) {
        super(null);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // 加载 JSON
        unitDetailJson = loadUnitDetails();

        // 左侧单位列表
        listContent = new JPanel();
        listContent.setLayout(new BoxLayout(listContent, BoxLayout.Y_AXIS));
        buildUnitEntries();

        listScroll = new JScrollPane(listContent);
        listScroll.setBorder(BorderFactory.createTitledBorder("单位列表"));
        listScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        listScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        listScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(listScroll);

        // 列表空白区域点击取消选择
        listContent.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                deselectUnit();
            }
        });

        // 右侧详情
        detailPanel = new UnitDexDetailPanel(unitDetailJson);
        detailScroll = new JScrollPane(detailPanel);
        detailScroll.setBorder(BorderFactory.createTitledBorder("单位详情"));
        detailScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        detailScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        detailScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(detailScroll);

        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                int w = getWidth();
                int h = getHeight();
                if (w <= 0 || h <= 0) return;
                int leftW = w / 5;
                int rightW = w - leftW;
                listScroll.setBounds(0, 0, leftW, h);
                detailScroll.setBounds(leftW, 0, rightW, h);
            }
        });

        // 全局点击取消选择
        Toolkit.getDefaultToolkit().addAWTEventListener(e -> {
            if (e instanceof java.awt.event.MouseEvent me
                    && me.getID() == java.awt.event.MouseEvent.MOUSE_PRESSED) {
                Component src = me.getComponent();
                if (!SwingUtilities.isDescendingFrom(src, listContent)) {
                    deselectUnit();
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    private void buildUnitEntries() {
        listContent.removeAll();
        for (int id = 0; id < UNIT_COUNT; id++) {
            UnitDexEntryPanel entry = new UnitDexEntryPanel(id, this::selectUnit);
            listContent.add(entry);
            if (id < UNIT_COUNT - 1) {
                listContent.add(Box.createVerticalStrut(1));
            }
        }
        listContent.add(Box.createVerticalGlue());
        listContent.revalidate();
        listContent.repaint();
    }

    private void selectUnit(int id) {
        if (id < 0 || id >= UNIT_COUNT) return;
        selectedUnitId = id;
        updateEntrySelection();
        detailPanel.showUnit(id);
        // 重置详情滚动到顶部
        SwingUtilities.invokeLater(() ->
                detailScroll.getVerticalScrollBar().setValue(0));
    }

    private void deselectUnit() {
        if (selectedUnitId < 0) return;
        selectedUnitId = -1;
        updateEntrySelection();
    }

    private void updateEntrySelection() {
        for (Component c : listContent.getComponents()) {
            if (c instanceof UnitDexEntryPanel entry) {
                entry.setSelected(entry.getUnitId() == selectedUnitId);
            }
        }
    }

    public void updateDarkMode() {
        detailPanel.updateDarkMode();
        buildUnitEntries();
        updateEntrySelection();
        repaint();
    }

    private static JsonNode loadUnitDetails() {
        try (InputStream in = UnitDexTab.class.getResourceAsStream("/unit_details.json")) {
            if (in == null) {
                System.err.println("UnitDexTab: unit_details.json 未找到");
                return null;
            }
            return new ObjectMapper().readTree(in);
        } catch (IOException e) {
            System.err.println("UnitDexTab: 加载 unit_details.json 失败: " + e.getMessage());
            return null;
        }
    }
}
