package org.example.GUI;

import javax.swing.*;

/**
 * JTextArea 子类，安装 FixedTextAreaUI 以修复自动换行时的光标向下取整问题。
 * 其余行为与普通 JTextArea 完全一致。
 */
public class FixedJTextArea extends JTextArea {

    public FixedJTextArea() {
        super();
    }

    public FixedJTextArea(int rows, int cols) {
        super(rows, cols);
    }

    @Override
    public void updateUI() {
        setUI(new FixedTextAreaUI());
    }
}
