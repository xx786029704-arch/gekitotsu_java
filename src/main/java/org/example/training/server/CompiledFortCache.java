package org.example.training.server;

import org.example.CompiledFort;
import org.example.Fort;
import org.example.Main;
import org.example.training.TrainingBattleRunner;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 有上限的 LRU 阵型编译缓存，避免固定对手在训练中被反复解析。
 */
final class CompiledFortCache {
    private final int capacity;
    private final Map<String, CompiledFort> entries;

    CompiledFortCache(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("cache capacity 必须大于 0");
        }
        this.capacity = capacity;
        this.entries = new LinkedHashMap<>(Math.min(capacity, 1024), 0.75F, true);
    }

    synchronized CompiledFort get(String code, String fieldName) {
        TrainingBattleRunner.validateCode(code, fieldName);
        CompiledFort cached = entries.get(code);
        if (cached != null) {
            return cached;
        }
        CompiledFort compiled = Main.compileFort(new Fort("", code));
        entries.put(code, compiled);
        if (entries.size() > capacity) {
            String oldest = entries.keySet().iterator().next();
            entries.remove(oldest);
        }
        return compiled;
    }

    synchronized int size() {
        return entries.size();
    }
}
