package org.example.elements.atk;

import org.example.GameTask;
import org.example.Main;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsDrop;

public class NinBullet extends Bullet {   //忍玉手里剑
    private int bounceCount = 0;

    public NinBullet(GameTask GAME, float X, float Y, int S) {   //初始化
        super(GAME, X, Y, S);
        r = 12.4F;
    }

    @Override
    public void step() {   //每帧逻辑
        if (x > 1920) {
            if (side != 1) {
                bounceCount = 99;
            }
            else {
                x = 0;
                bounceCount++;
            }
        }
        else if (x < 0) {
            if (side != 0) {
                bounceCount = 99;
            }
            else {
                x = 1920;
                bounceCount++;
            }
        }
        if (bounceCount > 1) {
            kill();
            return;
        }
        if (game.team[1 - side].hitTestPoint(x, y) || gei_flg == 2) {
            new HitsDrop(game, x, y, game.atk[side]);
            kill();
            return;
        }
        move();
    }
}
