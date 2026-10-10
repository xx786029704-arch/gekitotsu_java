package org.example.GUI;

import org.example.I18n;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 行为分析标签页。
 *
 * <p>上方为共享输入区（左侧原始阵型、右侧测试阵集）；下方为分析项选择区，每个分析项是一个标签页。
 * 目前仅有「贡献分析」，新增分析功能时继承 {@link BehaviorAnalysisPanel} 并在构造函数中 {@link #addItem} 注册。
 */
public class BehaviorAnalysisTab extends JPanel {

    private final BehaviorInputPanel inputPanel;
    private final JTabbedPane analysisTabs = new JTabbedPane();
    private final List<BehaviorAnalysisPanel> items = new ArrayList<>();

    public BehaviorAnalysisTab() {
        super(new BorderLayout(8, 4));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        inputPanel = new BehaviorInputPanel(this::onInputChanged);
        add(inputPanel, BorderLayout.NORTH);

        // 分析项区不设边框与标题，最大限度留给内容
        add(analysisTabs, BorderLayout.CENTER);

        // 分析项注册：新增功能在此追加 addItem(...)
        addItem(new ContributionAnalysisPanel(inputPanel::snapshot, inputPanel::setLocked));
        addItem(new MatchupProfilePanel(inputPanel::snapshot, inputPanel::setLocked));
        addItem(new BattlefieldPanel(inputPanel::snapshot, inputPanel::setLocked));

        onInputChanged(inputPanel.snapshot());
    }

    private void addItem(BehaviorAnalysisPanel item) {
        items.add(item);
        analysisTabs.addTab(I18n.t(item.titleKey()), item);
    }

    /** 共享输入（防抖后）变化时通知所有分析项。 */
    private void onInputChanged(BehaviorInput input) {
        for (BehaviorAnalysisPanel item : items) {
            item.onInputChanged(input);
        }
    }

    /** 深色模式切换时由 MainGUI 调用。 */
    public void updateDarkMode() {
        for (BehaviorAnalysisPanel item : items) {
            item.updateDarkMode();
        }
    }
}
