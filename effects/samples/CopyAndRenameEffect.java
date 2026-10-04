package org.example.effects;

import org.example.GUI.*;

import java.util.*;

/**
 * 【示例】重影幻视 Effect —— 复制指定百分比血量复制品到指定类型单位右侧指定距离处，再往阵名末尾或开头增加指定字符串。
 *
 * <h3>这个示例演示了自定义 Effect 开发的全部要点：</h3>
 * <ul>
 *   <li>实现 Effect 接口（getName / getDescription / getParameters / validate / execute）</li>
 *   <li>提供作者和版本元数据（getAuthor / getVersion）</li>
 *   <li>使用全部四种参数类型：INT、STRING、BOOLEAN、UNIT_ID</li>
 *   <li>编写参数校验逻辑（validate）</li>
 *   <li>实现纯函数式变换（不修改输入，返回新对象）</li>
 *   <li>通过 ServiceLoader 机制注册（META-INF/services）</li>
 * </ul>
 */
public class CopyAndRenameEffect implements Effect {

    // ========== 元数据 ==========

    @Override
    public String getName() {
        return "重影幻视";
    }

    @Override
    public String getDescription() {
        return "复制指定百分比血量复制品到指定类型单位右侧指定距离处，再往阵名末尾或开头增加指定字符串";
    }

    /** 作者署名 **/
    @Override
    public String getAuthor() {
        return "DeepSeek v4 Pro";
    }

    /** 版本号 **/
    @Override
    public String getVersion() {
        return "1.0.0";
    }

    // ========== 参数定义 ==========

    /**
     * 定义此 Effect 的可配置参数。
     * 返回列表的顺序决定了参数编辑对话框中各字段的显示顺序。
     * <p>
     * 参数类型说明：
     *   INT     —— 整数，UI 中为文本输入框
     *   STRING  —— 字符串，UI 中为文本输入框
     *   BOOLEAN —— 布尔值，UI 中为选项
     *   UNIT_ID —— 单位 ID，UI 中为下拉选择框（自动列出 64 个单位名称）
     */
    @Override
    public List<EffectParameter> getParameters() {
        return List.of(
                new EffectParameter("targetId", "目标单位", EffectParameter.Type.UNIT_ID, 0),
                new EffectParameter("extName", "字符串", EffectParameter.Type.STRING, ""),
                new EffectParameter("distance", "距离", EffectParameter.Type.INT, 5),
                new EffectParameter("hpPercent", "血量百分比", EffectParameter.Type.INT, 100),
                new EffectParameter("isFront", "加在前方（不选为加在后方）", EffectParameter.Type.BOOLEAN, false)
        );
    }

    // ========== 参数校验 ==========

    /**
     * 在执行前校验参数合法性。
     * 返回空列表表示校验通过；否则返回错误信息列表，
     * 多个错误会在 UI 中用分号连接显示。
     */
    @Override
    public List<String> validate(Map<String, Object> params) {
        List<String> errors = new ArrayList<>();
        if ((int) params.getOrDefault("hpPercent", 100) < 0) {
            errors.add("血量百分比必须大于0");
        }
        return errors; // 空列表 = 校验通过
    }

    // ========== 核心变换逻辑 ==========

    /**
     * 执行阵容变换。
     */
    @Override
    public Formation execute(Formation input, Map<String, Object> params) {
        //获取参数
        int targetId = (int) params.get("targetId");
        String extName = (String) params.get("extName");
        int distance = (int) params.get("distance");
        int hpPercent = (int) params.get("hpPercent");
        boolean isFront = (boolean) params.get("isFront");

        //变换操作
        List<Unit> news = new ArrayList<>();
        for (Unit u : input.units) {
            if (u.id == targetId) {
                Unit u2 = new Unit(u.id, u.x + distance, u.y, u.r);
                u2.hp = (int) (u.hp * hpPercent / 100);
                news.add(u2);
            }
        }
        input.units.addAll(news);
        input.name = isFront ? extName + input.name : input.name + extName;
        return input;
    }
}
