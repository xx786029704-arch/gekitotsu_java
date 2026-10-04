package org.example.GUI;

import org.example.Main;

import javax.swing.text.*;
import java.awt.*;

/**
 * 修复 WrappedPlainView 中 WrappedLine.viewToModel 的 round=false 问题。
 * 在父级拦截 viewToModel 结果，将 floor 行为修正为 round（最近边界）。
 */
public class FixedWrappedPlainView extends WrappedPlainView {

    public FixedWrappedPlainView(Element elem, boolean wordWrap) {
        super(elem, wordWrap);
    }

    @Override
    public int viewToModel(float fx, float fy, Shape a, Position.Bias[] bias) {
        int pos = super.viewToModel(fx, fy, a, bias);
        int start = getStartOffset();
        int end = getEndOffset();
        if (pos < start || pos >= end - 1) return pos;

        try {
            JTextComponent textComp = (JTextComponent) getContainer();
            if (textComp == null) return pos;

            Rectangle r1 = textComp.modelToView(pos).getBounds();
            Rectangle r2 = textComp.modelToView(pos + 1).getBounds();

            if (r1 != null && r2 != null) {
                float mid = (r1.x + r2.x) * 0.5f;
                if (fx > mid) return pos + 1;
            }
        } catch (BadLocationException ignored) {}
        return pos;
    }
}
