package org.example;

/** 从 CompiledFort 中裁剪掉指定索引的单位，生成新的 CompiledFort。 */
public final class FortTrimmer {

    private FortTrimmer() {}

    /**
     * 在原始阵型代码上删除指定索引的单位（0 基，对应第一个非核心单位），
     * 用于把裁剪后的阵型直接交给 Rust 模拟器，避免重编码引入差异。
     */
    public static String trimCode(String code, int[] removeIndices) {
        int unitCount = code.length() / 6 - 1;
        boolean[] remove = new boolean[unitCount];
        for (int idx : removeIndices) {
            if (idx < 0 || idx >= unitCount) {
                throw new IllegalArgumentException(I18n.t("err.indexOob", idx));
            }
            remove[idx] = true;
        }
        StringBuilder sb = new StringBuilder(code.length());
        sb.append(code, 0, 6);
        for (int i = 0; i < unitCount; i++) {
            if (!remove[i]) {
                sb.append(code, (i + 1) * 6, (i + 2) * 6);
            }
        }
        return sb.toString();
    }

    /**
     * 删除指定索引的单位，返回新的 CompiledFort（核心字段、单位顺序与随机种子保持不变，
     * 返回的数组不与 src 共享引用）。
     *
     * @param src           源阵型
     * @param removeIndices 要删除的单位索引（0 基，对应 src.type 数组）；顺序任意，重复自动去重
     * @throws IllegalArgumentException 索引越界时
     */
    public static CompiledFort trim(CompiledFort src, int[] removeIndices) {
        boolean[] remove = new boolean[src.unitCount];
        for (int idx : removeIndices) {
            if (idx < 0 || idx >= src.unitCount) {
                throw new IllegalArgumentException(I18n.t("err.indexOob", idx));
            }
            remove[idx] = true;
        }
        return trim(src, remove);
    }

    private static CompiledFort trim(CompiledFort src, boolean[] remove) {
        int kept = 0;
        for (int i = 0; i < src.unitCount; i++) {
            if (!remove[i]) {
                kept++;
            }
        }
        int[] type = new int[kept];
        int[] x = new int[kept];
        int[] y = new int[kept];
        int[] r = new int[kept];
        int[] seed = new int[kept];
        int j = 0;
        for (int i = 0; i < src.unitCount; i++) {
            if (remove[i]) {
                continue;
            }
            type[j] = src.type[i];
            x[j] = src.x[i];
            y[j] = src.y[i];
            r[j] = src.r[i];
            seed[j] = src.seed[i];
            j++;
        }
        return new CompiledFort(src.name, src.coreType, src.coreX, src.coreY,
                src.baseSeed, kept, type, x, y, r, seed);
    }
}
