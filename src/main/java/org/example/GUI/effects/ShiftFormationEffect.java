package org.example.GUI.effects;

import org.example.GUI.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : input.units) {
            Unit nu = new Unit(u.id, u.x + dx, u.y + dy, u.r);
            newUnits.add(nu);
        }
        return new Formation(input.name, newUnits);
    }
}
