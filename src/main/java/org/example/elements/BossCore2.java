package org.example.elements;

import org.example.GameTask;
import org.example.Main;
import org.example.Shape;
import org.example.Utils;
import org.example.elements.atk.BossMissileBullet;
import org.example.elements.hit.BossLaser2;

import java.util.List;

public class BossCore2 extends Core {      //BOSS核心类
    private int cnt;
    private int cnt2;
    private int nextStartTargetIndex=-1;

    public BossCore2(GameTask GAME, float X, float Y, int S) {
        super(GAME, X, Y, S);
        cnt=0;
    }

    @Override
    public void step() {     //照搬原版逻辑
        super.step();
        this.cnt++;
        if (this.cnt < 961 && this.cnt % 300 == 60) {
            new BossLaser2(game, this.x, this.y, this.side);
        }
        this.cnt2++;
        if (this.cnt2 > 300 && this.cnt2 % 3 == 0 && game.hp0_flg[1] == 0) {
            new BossMissileBullet(game, this.x, this.y, this.side, this.pickTargetId(), game.seeder.random(), game.seeder.random(), game.seeder.random(), game.seeder.random(), game.seeder.random());
            if (this.cnt2 >= 360) this.cnt2 = 0;
        }
    }

    private int pickTargetId() {   //挑选目标（我从简化的逻辑改成了和原版一样的逻辑，就没报错了）
        List<Shape> targets = game.unit[1 - side].getShapes();
        if (targets.isEmpty()) {
            return -1;
        }
        if (nextStartTargetIndex < 0 || nextStartTargetIndex >= targets.size()) {
            nextStartTargetIndex = targets.size() - 1;
        }
        int targetId = -1;
        int targetIndex = nextStartTargetIndex;
        while (targetIndex >= 0) {
            Shape wrk = targets.get(targetIndex);
            if (game.elements.get(wrk.id) != null) {
                targetId = wrk.id;
                nextStartTargetIndex = targetIndex - 1;
                break;
            }
            targetIndex--;
        }
        return targetId;
    }
}