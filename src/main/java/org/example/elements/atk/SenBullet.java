package org.example.elements.atk;

import org.example.Game;
import org.example.Utils;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;

public class SenBullet extends Bullet {   //战玉坦克
    private int hp = 3;
    private int cnt = 0;
    private boolean drop_flg = false;

    public SenBullet(Game game, float X, float Y, int S, int rotation) {   //初始化
        super(game, X, Y, S);
        gei_flg = 0;
        xs = Utils.cos(rotation) * 6F;
        ys = Utils.sin(rotation) * 6F;
    }

    @Override
    public void step() {   //每帧逻辑
        if (y < -1200 || x > 2560 || x < -640) {
            kill();
            return;
        }
        if (this.game.team[1 - side].hitTestPoint(x, y)) {
            hp--;
        }
        if (hp <= 0) {
            new HitsBomb(this.game, x, y, side);
            kill();
            return;
        }
        x = x + xs;
        y = y + ys;
        xySync();
        ys += 0.32F;
        if (y > 566) {
            drop_flg = true;
            y = 566;
            ys *= -0.125F;
            xs = 2 - 4 * side;
        }
        if (drop_flg) {
            cnt++;
        }
        if (drop_flg && cnt == 40) {
            cnt = 0;
            float cos = side == 1 ? 0.7071067811865475F : -0.7071067811865475F;
            new SenTamaBullet(this.game, x, y, side).setVecMult(cos, -0.7071067811865475F, 20).move();
        }
    }
}
