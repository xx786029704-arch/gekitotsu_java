package org.example.GUI.effects;

import org.example.GUI.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 阵容整体平移：将所有单位的坐标偏移 (dx, dy)。核心也一并偏移。 */
public class ShiftFormationEffect implements Effect {
    static {
        EffectRegistry.register(new ShiftFormationEffect());
    }

    @Override
    public String getName() { return "阵容平移"; }

    @Override
    public String getDescription() { return "将阵容中所有单位（含核心）沿 X/Y 轴平移指定距离。"; }

    @Override
    public List<EffectParameter> getParameters() {
        return List.of(
                new EffectParameter("dx", "X偏移", EffectParameter.Type.INT, 0),
                new EffectParameter("dy", "Y偏移", EffectParameter.Type.INT, 0)
        );
    }

    @Override
    public Formation execute(Formation input, Map<String, Object> params) {
        int dx = ((Number) params.getOrDefault("dx", 0)).intValue();
        int dy = ((Number) params.getOrDefault("dy", 0)).intValue();
        input.units = input.units.stream().peek(u -> {u.x += dx; u.y += dy;}).collect(Collectors.toList());
        return input;
    }
}
