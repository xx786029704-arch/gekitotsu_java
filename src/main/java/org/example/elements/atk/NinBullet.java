package org.example.elements.atk;

import org.example.Game;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsDrop;

public class NinBullet extends Bullet {   //忍玉手里剑
    private int bounceCount = 0;

    public NinBullet(Game game, float X, float Y, int S) {   //初始化
        super(game, X, Y, S);
        this.r = 12.4F;
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.x > 1920) {
            if (this.side != 1) {
                this.bounceCount = 99;
            }
            else {
                this.x = 0;
                this.bounceCount++;
            }
        }
        else if (this.x < 0) {
            if (this.side != 0) {
                this.bounceCount = 99;
            }
            else {
                this.x = 1920;
                this.bounceCount++;
            }
        }
        if (this.bounceCount > 1) {
            kill();
            return;
        }
        if (this.game.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            new HitsDrop(this.game, this.x, this.y, this.game.atk[this.side]);
            kill();
            return;
        }
        move();
    }
}
