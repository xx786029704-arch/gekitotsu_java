package org.example.GUI.effects;

import org.example.GUI.*;

import java.util.Map;
import java.util.stream.Collectors;

/** 角度规范化：将阵容中所有要塞壁类单位（含核心）的旋转角设为 0。 */
public class NormalizeAnglesEffect implements Effect {

    static {
        EffectRegistry.register(new NormalizeAnglesEffect());
    }

    @Override
    public String getName() { return org.example.I18n.t("effect.normalizeAngles.name"); }

    @Override
    public String getDescription() { return org.example.I18n.t("effect.normalizeAngles.desc"); }

    @Override
    public Formation execute(Formation input, Map<String, Object> params) {
        input.units = input.units.stream().peek(u -> {
            if (u.isWallLike()) u.r = 0;
        }).collect(Collectors.toList());
        return input;
    }
}
