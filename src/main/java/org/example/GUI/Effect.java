package org.example.GUI;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** 效果接口 —— 阵容处理流水线的最小处理单元。 */
public interface Effect {
    String getName();
    String getDescription();

    default List<EffectParameter> getParameters() {
        return Collections.emptyList();
    }

    /** 对输入阵容执行变换，返回新阵容。不得修改输入对象。 */
    Formation execute(Formation input, Map<String, Object> params);
}
