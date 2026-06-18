package org.example.GUI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** 效果注册表单例。效果类通过 static 块自注册，无需手动列举。 */
public class EffectRegistry {
    private static final List<Effect> effects = new ArrayList<>();
    private static boolean builtinsLoaded = false;

    /** 加载所有内置效果（触发各效果类的 static 注册块）。 */
    public static void loadBuiltins() {
        if (builtinsLoaded) return;
        builtinsLoaded = true;
        String[] builtins = {
                "org.example.GUI.effects.ReplaceUnitEffect",
                "org.example.GUI.effects.ShiftFormationEffect",
                "org.example.GUI.effects.RotateWallsEffect",
                "org.example.GUI.effects.FrontHealerRepairEffect",
                "org.example.GUI.effects.TopTargetEffect",
        };
        for (String className : builtins) {
            try {
                Class.forName(className);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public static void register(Effect effect) {
        effects.add(effect);
    }

    public static List<Effect> getAll() {
        return Collections.unmodifiableList(effects);
    }

    /** 按名称模糊搜索。 */
    public static List<Effect> search(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAll();
        }
        String lower = query.toLowerCase().trim();
        return effects.stream()
                .filter(e -> e.getName().toLowerCase().contains(lower)
                        || e.getDescription().toLowerCase().contains(lower))
                .collect(Collectors.toList());
    }
}
