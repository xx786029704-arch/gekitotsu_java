package org.example.GUI;

import java.awt.Color;

/** Trace 和 TraceWall 的公共接口，供 TraceItemPanel 渲染和交互使用。 */
public interface ListItem {
    String getName();
    void setName(String name);
    Color getColor();
    void setColor(Color color);
    boolean isVisible();
    void setVisible(boolean visible);
}
