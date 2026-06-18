package org.example.GUI.effects;

import org.example.GUI.*;
import org.example.Main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 单位整体替换：将阵容中所有指定类型的单位替换为另一类型，保留坐标和旋转角。 */
public class ReplaceUnitEffect implements Effect {
    static {
        EffectRegistry.register(new ReplaceUnitEffect());
    }

    @Override
    public String getName() { return "单位替换"; }

    @Override
    public String getDescription() { return "将一种单位整体替换为另一种单位，保留原有坐标和旋转角。"; }

    @Override
    public List<EffectParameter> getParameters() {
        return List.of(
                new EffectParameter("sourceId", "源单位ID", EffectParameter.Type.STRING, ""),
                new EffectParameter("targetId", "目标单位ID", EffectParameter.Type.STRING, "")
        );
    }

    @Override
    public Formation execute(Formation input, Map<String, Object> params) {
        String sourceId = (String) params.get("sourceId");
        String targetId = (String) params.get("targetId");
        if (sourceId.length() != 1) {
            sourceId = "Z";
        }
        if (targetId.length() != 1) {
            targetId = "Z";
        }
        int srcId = Main.pskey.indexOf(sourceId);
        int tgtId = Main.pskey.indexOf(targetId);

        List<Unit> newUnits = new ArrayList<>();
        for (Unit u : input.units) {
            Unit nu;
            if (u.id == srcId) {
                if (tgtId == 61) continue;
                nu = new Unit(tgtId, u.x, u.y, u.r);
            } else {
                nu = new Unit(u.id, u.x, u.y, u.r);
            }
            newUnits.add(nu);
        }
        return new Formation(input.name, newUnits);
    }
}
