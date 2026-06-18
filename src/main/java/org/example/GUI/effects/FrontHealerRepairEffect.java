package org.example.GUI.effects;

import org.example.GUI.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 前置愈缮玉：将所有愈玉移至核心后最前方，缮玉紧随其后。 */
public class FrontHealerRepairEffect implements Effect {
    static {
        EffectRegistry.register(new FrontHealerRepairEffect());
    }

    @Override
    public String getName() { return "前置愈缮玉"; }

    @Override
    public String getDescription() { return "将所有愈玉(id18)移至核心后最前方，缮玉(id17)紧随其后。"; }

    @Override
    public Formation execute(Formation input, Map<String, Object> params) {
        List<Unit> src = input.units;
        List<Unit> newUnits = new ArrayList<>();
        // 核心保持在索引0
        Unit core = src.get(0);
        newUnits.add(new Unit(core.id, core.x, core.y, core.r));

        List<Unit> healers = new ArrayList<>();
        List<Unit> repairers = new ArrayList<>();
        List<Unit> others = new ArrayList<>();

        for (int i = 1; i < src.size(); i++) {
            Unit u = src.get(i);
            if (u.id == 18) {
                healers.add(new Unit(u.id, u.x, u.y, u.r));
            } else if (u.id == 17) {
                repairers.add(new Unit(u.id, u.x, u.y, u.r));
            } else {
                others.add(new Unit(u.id, u.x, u.y, u.r));
            }
        }

        newUnits.addAll(healers);
        newUnits.addAll(repairers);
        newUnits.addAll(others);
        return new Formation(input.name, newUnits);
    }
}
