package org.example.elements.atk;

import org.example.GameTask;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsDrop;

public class KanBullet extends Bullet {   //贯玉子弹

    public KanBullet(GameTask GAME, float X, float Y, int S) {   //初始化
        super(GAME, X, Y, S);
    }

    @Override
    public void step() {   //每帧逻辑
        if (y > 570 || y < -600 || x > 1920 || x < 0) {
            kill();
            return;
        }
        if (game.atk[1 - side].hitTestPoint(x, y) || gei_flg == 2) {
            hit();
            return;
        }
        if (game.fort[1 - side].hitTestPoint(x, y) || game.shield[1 - side].hitTestPoint(x, y)) {
            new HitsDrop(game, x, y, game.atk[side]);
        }
        move();
    }
}
