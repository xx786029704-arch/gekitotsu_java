package org.example.GUI;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;

/** 加载 formula.json 中的分段公式，按 (sp1, sp2, t) 快速求值双方要塞坐标。 */
public class FormulaTable {

    public static final int DIM_P1_X = 0;
    public static final int DIM_P2_X = 1;
    public static final int DIM_P1_Y = 2;
    public static final int DIM_P2_Y = 3;

    private static final String[] DIM_KEYS = {"px", "qx", "py", "qy"};
    private static final int SPEED_LEVELS = 16;

    /** [dim][sp1][sp2][seg][4] — 每段为 [t0, a, v0, x0] */
    private final double[][][][][] data;
    private boolean loaded;

    public FormulaTable() {
        data = new double[4][SPEED_LEVELS][SPEED_LEVELS][][];
        loaded = loadFromResource();
    }

    public boolean isLoaded() {
        return loaded;
    }

    private boolean loadFromResource() {
        try (InputStream in = getClass().getResourceAsStream("/formula.json")) {
            if (in == null) {
                System.err.println("FormulaTable: formula.json 未找到");
                return false;
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(in);

            for (int dim = 0; dim < 4; dim++) {
                JsonNode dimNode = root.get(DIM_KEYS[dim]);
                if (dimNode == null || !dimNode.isArray()) {
                    System.err.println("FormulaTable: 缺少键 " + DIM_KEYS[dim]);
                    return false;
                }
                for (int sp1 = 0; sp1 < SPEED_LEVELS; sp1++) {
                    JsonNode sp1Node = dimNode.get(sp1);
                    for (int sp2 = 0; sp2 < SPEED_LEVELS; sp2++) {
                        JsonNode sp2Node = sp1Node.get(sp2);
                        int segCount = sp2Node.size();
                        double[][] segs = new double[segCount][4];
                        for (int s = 0; s < segCount; s++) {
                            JsonNode segNode = sp2Node.get(s);
                            segs[s][0] = segNode.get(0).asDouble(); // t0
                            segs[s][1] = segNode.get(1).asDouble(); // a
                            segs[s][2] = segNode.get(2).asDouble(); // v0
                            segs[s][3] = segNode.get(3).asDouble(); // x0
                        }
                        data[dim][sp1][sp2] = segs;
                    }
                }
            }
            return true;
        } catch (IOException e) {
            System.err.println("FormulaTable: 加载失败 - " + e.getMessage());
            return false;
        }
    }

    /** 同时求 4 个维度的坐标，返回 [1P_x, 2P_x, 1P_y, 2P_y]。 */
    public double[] evaluate(int sp1, int sp2, double t) {
        return new double[]{
            evalDim(DIM_P1_X, sp1, sp2, t),
            evalDim(DIM_P2_X, sp1, sp2, t),
            evalDim(DIM_P1_Y, sp1, sp2, t),
            evalDim(DIM_P2_Y, sp1, sp2, t),
        };
    }

    /** 求单个维度的坐标。 */
    public double evalDim(int dim, int sp1, int sp2, double t) {
        double[][] segs = data[dim][sp1][sp2];
        int lo = 0, hi = segs.length;

        // 二分查找最大的 t0 ≤ t 的段
        while (lo + 1 < hi) {
            int mid = (lo + hi) >>> 1;
            if (segs[mid][0] <= t) lo = mid;
            else hi = mid;
        }

        double[] s = segs[lo];
        double dt = t - s[0];
        // x(t) = a·dt²/2 + v0·dt + x0
        return Math.round(s[1] * dt * dt / 2.0 + s[2] * dt + s[3]);
    }

    /** 查找近突击壁第 times 次触发帧（1P x 分段中第 times 个 v₀<0 的段的 t_start）。 */
    public int findNearTriggerTime(int sp1, int sp2, int times) {
        double[][] segs = data[DIM_P1_X][sp1][sp2];
        int found = 0;
        for (double[] seg : segs) {
            if (seg[2] < 0) { // v₀ < 0 表示与对方要塞碰撞后退
                found++;
                if (found >= times) return (int) seg[0];
            }
        }
        return -1;
    }
}
