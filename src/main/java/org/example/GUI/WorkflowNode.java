package org.example.GUI;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 工作流图中的一个节点，持有 Effect 实例及其运行时参数和状态。 */
public class WorkflowNode {
    public final String id;
    public final Effect effect;
    public boolean enabled = true;
    public String error;
    public final Map<String, Object> paramValues = new HashMap<>();

    public WorkflowNode(Effect effect) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.effect = effect;
        for (EffectParameter p : effect.getParameters()) {
            paramValues.put(p.key(), p.defaultValue());
        }
    }

    /** 创建该节点的默认渲染组件。子类可重写以自定义外观。 */
    public JPanel createComponent(Runnable onChanged, Runnable onDelete,
                                  Runnable onSelect, Runnable onDragStart,
                                  Runnable onDragUpdate, Runnable onDragEnd) {
        return new NodeComponent(this, onChanged, onDelete, onSelect,
                onDragStart, onDragUpdate, onDragEnd);
    }
}
