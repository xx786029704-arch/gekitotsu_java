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
    public String getName() { return org.example.I18n.t("effect.shift.name"); }

    @Override
    public String getDescription() { return org.example.I18n.t("effect.shift.desc"); }

    @Override
    public List<EffectParameter> getParameters() {
        return List.of(
                new EffectParameter("dx", org.example.I18n.t("effect.shift.dx"), EffectParameter.Type.INT, 0),
                new EffectParameter("dy", org.example.I18n.t("effect.shift.dy"), EffectParameter.Type.INT, 0)
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
