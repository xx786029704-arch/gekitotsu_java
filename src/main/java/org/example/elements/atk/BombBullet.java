package org.example.elements.atk;

import org.example.Game;
import org.example.Round;
import org.example.Utils;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;

public class BombBullet extends Bullet {   //爆玉子弹
    public BombBullet(Game game, float X, float Y, int S) {   //初始化
        super(game, X, Y, S);
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y > 570 || this.game.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            new HitsBomb(this.game, this.x, this.y, this.side);
            kill();
            return;
        }
        move();
    }
}
