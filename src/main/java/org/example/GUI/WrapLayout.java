package org.example.GUI;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * 可换行的 FlowLayout：按容器当前宽度计算多行高度，
 * 实际高度不足时自动增高容器，避免窄窗口下第二行内容被裁掉。
 */
public class WrapLayout extends FlowLayout {

    public WrapLayout() {
        super();
    }

    public WrapLayout(int align) {
        super(align);
    }

    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= getHgap() + 1;
        if (minimum.width < 0) {
            minimum.width = 0;
        }
        return minimum;
    }

    @Override
    public void layoutContainer(Container target) {
        super.layoutContainer(target);
        Dimension preferred = preferredLayoutSize(target);
        if (target.getHeight() != preferred.height) {
            target.setSize(target.getWidth(), preferred.height);
            Container parent = target.getParent();
            if (parent != null) {
                parent.revalidate();
            }
        }
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            if (targetWidth == 0) {
                targetWidth = Integer.MAX_VALUE;
            }
            int hgap = getHgap();
            int vgap = getVgap();
            Insets insets = target.getInsets();
            int horizontalInsetsAndGap = insets.left + insets.right + hgap * 2;
            int maxWidth = targetWidth - horizontalInsetsAndGap;
            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;
            int members = target.getComponentCount();
            for (int i = 0; i < members; i++) {
                Component m = target.getComponent(i);
                if (!m.isVisible()) {
                    continue;
                }
                Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                if (rowWidth + d.width > maxWidth) {
                    addRow(dim, rowWidth, rowHeight);
                    rowWidth = 0;
                    rowHeight = 0;
                }
                if (rowWidth != 0) {
                    rowWidth += hgap;
                }
                rowWidth += d.width;
                rowHeight = Math.max(rowHeight, d.height);
            }
            addRow(dim, rowWidth, rowHeight);
            dim.width += horizontalInsetsAndGap;
            dim.height += insets.top + insets.bottom + vgap * 2;
            Container scrollPane = javax.swing.SwingUtilities.getAncestorOfClass(
                    javax.swing.JScrollPane.class, target);
            if (scrollPane != null && target.isValid()) {
                dim.width -= getHgap() + 1;
            }
            return dim;
        }
    }

    private void addRow(Dimension dim, int rowWidth, int rowHeight) {
        dim.width = Math.max(dim.width, rowWidth);
        if (dim.height > 0) {
            dim.height += getVgap();
        }
        dim.height += rowHeight;
    }
}
