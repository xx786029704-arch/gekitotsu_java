package org.example.elements.atk;

import org.example.GameTask;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBombMult;

public class SenTamaBullet extends Bullet {   //战玉子弹
    private int hp = 3;

    public SenTamaBullet(GameTask GAME, float X, float Y, int S) {   //初始化
        super(GAME, X, Y, S);
    }

    @Override
    public void step() {   //每帧逻辑
        if (y > 570) {
            hp = 0;
        }
        else if (game.team[1 - side].hitTestPoint(x, y) || gei_flg == 2) {
            if (game.fort[1 - side].hitTestPoint(x, y) || game.shield[1 - side].hitTestPoint(x, y)) {
                hp = 0;
            }
            else {
                hp--;
            }
        }
        x = x + xs;
        y = y + ys;
        xySync();
        ys = ys + 0.32F;
        if (hp <= 0) {
            new HitsBombMult(game, x, y, side, 0.5F);
            kill();
        }
    }
}
