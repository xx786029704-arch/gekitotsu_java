package org.example.GUI.effects;

import org.example.GUI.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 顶置的玉：将所有"的玉"(id=42)的 y 坐标设为 0。 */
public class TopTargetEffect implements Effect {
    static {
        EffectRegistry.register(new TopTargetEffect());
    }

    @Override
    public String getName() { return "顶置的玉"; }

    @Override
    public String getDescription() { return "将所有「的玉」(id42) 的 y 坐标设为 0。"; }

    @Override
    public Formation execute(Formation input, Map<String, Object> params) {
        input.units = input.units.stream().peek(u -> u.y = u.id == 42 ? 0 : u.y).collect(Collectors.toList());
        return input;
    }
}
