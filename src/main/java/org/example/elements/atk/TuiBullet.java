package org.example.elements.atk;

import org.example.GameTask;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;

public class TuiBullet extends Bullet {   //坠玉主弹
    private int hp = 3;
    private int cnt = 0;
    private boolean drop_flg = false;

    public TuiBullet(GameTask GAME, float X, float Y, int S, float cos_rot, float sin_rot) {   //初始化
        super(GAME, X, Y, S);
        gei_flg = 0;
        xs = cos_rot * 6F;
        ys = sin_rot * 6F;
    }

    @Override
    public void step() {   //每帧逻辑
        if (y < -1200 || x > 2560 || x < -640) {
            kill();
            return;
        }
        if (game.team[1 - side].hitTestPoint(x, y)) {
            hp--;
        }
        if (hp <= 0) {
            new HitsBomb(game, x, y, side);
            kill();
            return;
        }
        x = x + xs;
        y = y + ys;
        xySync();
        ys = ys + 0.32F;
        if (y > 566) {
            drop_flg = true;
            y = 566;
            ys *= -0.125F;
            xs = 3 - 6 * side;
        }
        if (drop_flg) {
            cnt++;
        }
        if (cnt == 40 || cnt == 44 || cnt == 60 || cnt == 64 || cnt == 80 || cnt == 84) {
            if (cnt == 84) {
                cnt = 0;
            }
            new TuiMissileBullet(game, x, y - 20F, side);
        }
    }
}
