package org.example.GUI.effects;

import org.example.GUI.*;

import java.util.*;
import java.util.Map;
import java.util.stream.Collectors;

/** 墙壁随机旋转：将阵容中所有墙壁类单位的旋转角设为随机值。 */
public class RotateWallsEffect implements Effect {

    static {
        EffectRegistry.register(new RotateWallsEffect());
    }

    @Override
    public String getName() { return org.example.I18n.t("effect.rotateWalls.name"); }

    @Override
    public String getDescription() { return org.example.I18n.t("effect.rotateWalls.desc"); }

    @Override
    public Formation execute(Formation input, Map<String, Object> params) {
        Random rng = new Random();
        input.units = input.units.stream().peek(u -> u.r = u.isWall() ? rng.nextInt(360) : u.r).collect(Collectors.toList());
        return input;
    }
}
