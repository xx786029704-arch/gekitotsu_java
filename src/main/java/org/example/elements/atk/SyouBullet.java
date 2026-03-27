package org.example.elements.atk;

import org.example.*;
import org.example.Rectangle;
import org.example.elements.hit.HitsDrop;

import java.awt.*;

public class SyouBullet extends Rectangle {   //障玉障壁
    private final int side;
    private final int id;
    private int hp = 10;
    private int cnt = -7;
    private float power = 20F;
    private final float cosRot;
    private final float sinRot;
    private boolean active = false;
    private final GameTask game;

    public SyouBullet(GameTask GAME, float X, float Y, int S, float cos_rot, float sin_rot) {   //初始化
        super(X, Y, -14, -14, 28, 28);
        game = GAME;
        side = S;
        cosRot = cos_rot;
        sinRot = sin_rot;
        id = game.addElement(this);
    }

    @Override
    public void step() {   //每帧逻辑
        if (game.team[1 - side].hitTestPoint(x, y)) {
            hp--;
            if (game.fort[1 - side].hitTestPoint(x, y) || game.shield[1 - side].hitTestPoint(x, y)) {
                hp = 0;
            }
        }
        if (y > 570 || y < -600 || x > 1920 || x < 0 || hp <= 0 || cnt >= 159) {
            if (active) {
                new HitsDrop(game, x, y, game.atk[side]);
            }
            kill();
            return;
        }
        if (power > 0) {
            power--;
            x += cosRot * power;
            y += sinRot * power;
            xySync();
        } else {
            cnt++;
            if (cnt == 0){
                activate();
            }
        }
    }

    private void activate() {
        if (!active) {
            active = true;
            game.unit[side].addShape(this);
        }
    }

    private void kill() {
        game.elements.remove(id);
        if (active) {
            game.unit[side].removeShape(this);
        }
    }
}
