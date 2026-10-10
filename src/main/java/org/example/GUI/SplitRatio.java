package org.example.GUI;

import javax.swing.*;
import java.awt.event.HierarchyEvent;

/**
 * 为 {@link JSplitPane} 设置首次显示时的分隔线比例。
 *
 * <p>组件尚未显示时宽高为 0，按比例定位无效，因此在首次显示后延后一次生效；
 * 之后分隔线由用户拖动，窗口缩放时由 {@code setResizeWeight} 保持比例。
 */
final class SplitRatio {

    private SplitRatio() {
    }

    static void applyOnFirstShow(JSplitPane split, double ratio) {
        boolean[] applied = {false};
        split.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0
                    && split.isShowing() && !applied[0]) {
                applied[0] = true;
                SwingUtilities.invokeLater(() -> split.setDividerLocation(ratio));
            }
        });
    }
}
