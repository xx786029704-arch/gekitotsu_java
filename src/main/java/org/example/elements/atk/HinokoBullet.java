package org.example.elements.atk;

import org.example.Game;
import org.example.Utils;
import org.example.elements.Bullet;
public class HinokoBullet extends Bullet {   //花玉小子弹
    private int cnt = 0;

    public HinokoBullet(Game game, float X, float Y, int S, int rotation) {   //初始化
        super(game, X, Y, S);
        this.x = X + Utils.cos(rotation) * 14F;
        this.y = Y + Utils.sin(rotation) * 14F;
        xySync();
    }

    @Override
    public void step() {   //每帧逻辑
        this.cnt++;
        if (this.y > 570 || this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (this.game.team[1 - this.side].hitTestPoint(this.x, this.y) || this.cnt > 20 || this.gei_flg == 2) {
            hit();
            return;
        }
        this.xs *= 0.8F;
        this.ys *= 0.8F;
        move();
    }
}
