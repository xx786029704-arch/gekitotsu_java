package org.example.elements.atk;

import org.example.GameTask;
import org.example.Main;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsDrop;

public class HaneBullet extends Bullet {   //跳玉子弹
    private int bounceCount = 0;

    public HaneBullet(GameTask GAME, float X, float Y, int S) {   //初始化
        super(GAME, X, Y, S);
        r *= 0.8F;
    }

    @Override
    public void step() {   //每帧逻辑
        if (y > 570 || y < -600) {
            ys = -ys;
            bounceCount++;
        }
        if (x > 1920 || x < 0) {
            xs = -xs;
            bounceCount++;
        }
        if (bounceCount > 4) {
            kill();
            return;
        }
        if (game.team[1 - side].hitTestPoint(x, y) || gei_flg == 2) {
            new HitsDrop(game, x, y, game.atk[side]);
            kill();
            return;
        }
        move();
    }
}
