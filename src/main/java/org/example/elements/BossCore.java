package org.example.elements;

import org.example.elements.hit.BossLaser;

public class BossCore extends Core {      //BOSS核心类
    private int cnt;

    public BossCore(float X, float Y, int S) {
        super(X, Y,S);
        cnt=0;
    }


    @Override
    public void step() {     //照搬原版逻辑
        super.step();
        this.cnt++;
        if (this.cnt < 2761 && this.cnt % 900 == 60) {
            new BossLaser(this.x, this.y, this.side);
        }
    }
}