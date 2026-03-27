package org.example.elements.atk;

import org.example.GameTask;
import org.example.Main;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsDrop;

public class TonBullet extends Bullet {   //弹玉弹球
    private int hp = 3;
    private int bounceCount = 0;

    public TonBullet(GameTask GAME, float X, float Y, int S) {   //初始化
        super(GAME, X, Y, S);
        r = r * 1.2F;
    }

    @Override
    public void step() {   //每帧逻辑
        if (y < -1200 || x > 2560 || x < -640) {
            new HitsDrop(game, x, y, game.atk[side]);
            kill();
            return;
        }
        if (y > 570) {
            y = 570;
            ys = -ys;
            bounceCount++;
            if (bounceCount >= 5) {
                hp = 0;
            }
        }
        else if (game.team[1 - side].hitTestPoint(x, y) || gei_flg == 2) {
            hp--;
            if (game.fort[1 - side].hitTestPoint(x, y) || game.shield[1 - side].hitTestPoint(x, y)) {
                hp = 0;
            }
        }
        if (hp <= 0) {
            new HitsDrop(game, x, y, game.atk[side]);
            kill();
            return;
        }
        move();
    }
}
