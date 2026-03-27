package org.example.elements.atk;

import org.example.GameTask;
import org.example.elements.Bullet;
public class HanabiBullet extends Bullet {   //花玉子弹
    private int cnt = 0;
    private final int rot;

    public HanabiBullet(GameTask GAME, float X, float Y, int S, int R) {   //初始化
        super(GAME, X, Y, S);
        this.rot = R;
        this.r = this.r * 0.8F;
    }

    @Override
    public void step() {   //每帧逻辑
        this.cnt++;
        if (this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (this.y > 570 || game.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2 || this.cnt > 60) {
            hit();
            return;
        }
        move();
    }

    @Override
    public boolean hit() {   //触发散射
        for (int i = 0; i < 20; i++) {
            int rot = this.rot + 18 * i;
            new HinokoBullet(game, this.x, this.y, this.side, rot).setVecR(rot, 30);
        }
        return super.hit();
    }
}
