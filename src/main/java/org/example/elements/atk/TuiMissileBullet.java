package org.example.elements.atk;

import org.example.GameTask;
import org.example.Utils;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBombMult;

public class TuiMissileBullet extends Bullet {   //坠玉导弹
    private float speed = 15F;
    private float cos_rot;
    private float sin_rot;

    public TuiMissileBullet(GameTask GAME, float X, float Y, int S) {   //初始化
        super(GAME, X, Y, S);
        cos_rot = side == 1 ? -.5F : .5F;
        sin_rot = -0.8660254037844386F;
        xs = cos_rot * 15;
        ys = sin_rot * 15;
    }

    @Override
    public void step() {   //每帧逻辑
        if (y < -1200 || x > 2560 || x < -640) {
            kill();
            return;
        }
        if (game.team[1 - side].hitTestPoint(x, y) || y > 570 || gei_flg == 2) {
            new HitsBombMult(game, x, y, side, 0.5F);
            kill();
            return;
        }
        if (speed < 20F) {
            speed = speed + 1F;
        }
        xs = cos_rot * speed;
        ys = sin_rot * speed;
        x = x + xs;
        y = y + ys;
        xySync();
    }

    @Override
    public void reflect(int from_rot){
        betray();
        cos_rot = Utils.cos(from_rot);
        sin_rot = Utils.sin(from_rot);
        this.xs = cos_rot * this.speed;
        this.ys = sin_rot * this.speed;
    }
}
