package org.example.GUI;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTextAreaUI;
import javax.swing.text.*;

/**
 * 自定义 TextAreaUI，在 lineWrap 时使用 FixedWrappedPlainView 替代
 * WrappedPlainView，修复光标向下取整的问题。
 */
public class FixedTextAreaUI extends BasicTextAreaUI {

    @Override
    public View create(Element elem) {
        JTextComponent c = getComponent();
        if (c instanceof JTextArea) {
            JTextArea area = (JTextArea) c;
            if (area.getLineWrap()) {
                return new FixedWrappedPlainView(elem, area.getWrapStyleWord());
            } else {
                return new PlainView(elem);
            }
        }
        return null;
    }
}
