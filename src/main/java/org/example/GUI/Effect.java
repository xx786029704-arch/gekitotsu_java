package org.example.GUI;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** 效果接口 —— 阵容处理流水线的最小处理单元。 */
public interface Effect {
    String getName();
    String getDescription();

    default String getAuthor() { return ""; }
    default String getVersion() { return ""; }

    default List<EffectParameter> getParameters() {
        return Collections.emptyList();
    }

    /** 校验参数合法性。返回空列表表示通过，否则返回人类可读的错误信息列表。 */
    default List<String> validate(Map<String, Object> params) {
        return Collections.emptyList();
    }

    /** 对输入阵容执行变换，返回新阵容。*/
    Formation execute(Formation input, Map<String, Object> params);
}
