package org.example.elements.atk;

import org.example.Game;
import org.example.Round;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsDrop;

public class TonBullet extends Bullet {   //弹玉弹球
    private int hp = 3;
    private int bounceCount = 0;

    public TonBullet(Game game, float X, float Y, int S) {   //初始化
        super(game, X, Y, S);
        this.r = this.r * 1.2F;
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y < -1200 || this.x > 2560 || this.x < -640) {
            new HitsDrop(this.game, this.x, this.y, this.game.atk[this.side]);
            kill();
            return;
        }
        if (this.y > 570) {
            this.y = 570;
            this.ys = -this.ys;
            this.bounceCount++;
            if (this.bounceCount >= 5) {
                this.hp = 0;
            }
        }
        else if (this.game.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            this.hp--;
            if (this.game.fort[1 - this.side].hitTestPoint(this.x, this.y) || this.game.shield[1 - this.side].hitTestPoint(this.x, this.y)) {
                this.hp = 0;
            }
        }
        if (this.hp <= 0) {
            new HitsDrop(this.game, this.x, this.y, this.game.atk[this.side]);
            kill();
            return;
        }
        move();
    }
}
