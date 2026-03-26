package org.example.elements.atk;

import org.example.Game;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsDrop;

public class KanBullet extends Bullet {   //贯玉子弹

    public KanBullet(Game game, float X, float Y, int S) {   //初始化
        super(game, X, Y, S);
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y > 570 || this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (this.game.atk[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            hit();
            return;
        }
        if (this.game.fort[1 - this.side].hitTestPoint(this.x, this.y) || this.game.shield[1 - this.side].hitTestPoint(this.x, this.y)) {
            new HitsDrop(this.game, this.x, this.y, this.game.atk[this.side]);
        }
        move();
    }
}
