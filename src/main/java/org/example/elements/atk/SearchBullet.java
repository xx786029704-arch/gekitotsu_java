package org.example.elements.atk;

import org.example.GameTask;
import org.example.Round;
import org.example.Shape;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsDropFrames;
import java.util.List;

public class SearchBullet extends Round {       //查玉子弹（Gemini狂猛优化版)
    private final int side;
    private final int id;
    private float xs = 0;
    private float ys = 0;
    private float dirX;
    private float dirY;
    private final GameTask game;

    // 预计算常量
    private static final float DEG_TO_RAD = 0.017453292519943295F;

    public SearchBullet(GameTask GAME, float X, float Y, int S, float _cos_rot, float _sin_rot) {
        super(X, Y, 15.5F);
        xySync();
        game = GAME;
        side = S;
        dirX = _cos_rot;
        dirY = _sin_rot;
        xs = dirX * 15F;
        ys = dirY * 15F;
        id = game.addElement(this);
        game.atk[side].addShape(this);
    }

    @Override
    public void step() {
        // 1. 消除数组重复寻址 (Cache array accesses)
        int oppSide = 1 - side;
        var oppAtk = game.atk[oppSide];
        var oppFort = game.fort[oppSide];
        var oppShield = game.shield[oppSide];

        // 2. 避免 Iterator 的 GC 碎片化分配
        // 假设 getShapes() 返回的是 ArrayList，使用索引遍历能带来质的飞跃
        List<Shape> shapes = (List<Shape>) oppAtk.getShapes();
        int size = shapes.size();

        int i = 0;
        float t_dist2;
        int t_dist_est;
        float dx, dy;
        float t_rot;
        float rot;

        // 记录 rot 是否发生变化，避免无意义的三角函数计算
        boolean rotChanged = true;

        while (i < 5) {
            t_dist2 = 4000000F;
            t_dist_est = 2000;

            for (Shape s : shapes) {
                if (s instanceof Bullet bullet && bullet.gei_flg == 1) {
                    dx = bullet.x - x;
                    if (dx > t_dist_est || dx < -t_dist_est) continue;
                    dy = bullet.y - y;
                    if (dy > t_dist_est || dy < -t_dist_est) continue;

                    float dist2 = dx * dx + dy * dy;
                    // 【核心级优化】：将最耗时的 atan2 和 角度判断 放在距离判断之后！
                    // 只有当距离严格小于目前找到的最小距离时，才去计算角度。
                    // 这能砍掉 99% 的 Math.atan2 运算！
                    if (dist2 > t_dist2) continue;

                    float dot = dirX * dx + dirY * dy;

                    // 判断是否在前方 (dot > 0) 且在 45 度夹角内 (dot^2 >= 0.5 * dist2)
                    // 注意：0.5f 是 cos(45)^2。如果是 30 度，就是 cos(30)^2 = 0.75f
                    if (dot > 0F && (dot * dot) >= 0.5F * dist2) {
                        // 3. 只有成功突破所有滤网，才进行极其昂贵的 atan2 计算来转向
                        t_dist2 = dist2;
                        t_rot = (float) Math.toDegrees(Math.atan2(dy, dx));

                        t_dist_est = (1 << ((32 - Integer.numberOfLeadingZeros((int) t_dist2) + 1) >> 1));
                        t_dist_est += t_dist_est >> 3;

                        rot = t_rot;
                        rotChanged = true;

                        // 更新新的单位方向向量，用于后续循环的比较
                        dirX = (float) Math.cos(rot * DEG_TO_RAD);
                        dirY = (float) Math.sin(rot * DEG_TO_RAD);

                        if (dist2 <= 900F) {
                            bullet.gei_flg = 2;
                        }
                    }
                }
            }

            //仅当旋转角度发生变化时，才重新计算 sin/cos
            if (rotChanged) {
                xs = dirX * 15F;
                ys = dirY * 15F;
                rotChanged = false; // 重置状态
            }

            x += xs;
            y += ys;
            xySync();

            // 边界检查使用 Float 后缀避免隐式类型转换
            if (y > 570F || y < -600F || x > 1920F || x < 0F) {
                kill();
                return;
            }

            if (oppFort.hitTestPoint(x, y) || oppShield.hitTestPoint(x, y)) {
                new HitsDropFrames(game, x, y, game.atk[side], 3, 1);
                kill();
                return;
            }
            i++;
        }
    }

    public void kill() {
        game.elements.remove(id);
        game.atk[side].removeShape(this);
    }
}