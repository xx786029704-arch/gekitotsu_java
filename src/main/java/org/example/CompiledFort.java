package org.example;

public final class CompiledFort {
    final String name;
    final int coreType;
    final int coreX, coreY;
    final int baseSeed;
    final int unitCount;

    // SoA（结构分离，比对象数组更快）
    final int[] type;
    final int[] x;
    final int[] y;
    final int[] r;
    final int[] seed;

    CompiledFort(String name,
                 int coreType, int coreX, int coreY,
                 int baseSeed,
                 int unitCount,
                 int[] type, int[] x, int[] y, int[] r, int[] seed) {

        this.name = name;
        this.coreType = coreType;
        this.coreX = coreX;
        this.coreY = coreY;
        this.baseSeed = baseSeed;
        this.unitCount = unitCount;
        this.type = type;
        this.x = x;
        this.y = y;
        this.r = r;
        this.seed = seed;
    }
}
