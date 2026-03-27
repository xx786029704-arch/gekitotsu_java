package org.example.elements.atk;

import org.example.GameTask;
import org.example.Main;
import org.example.Round;
import org.example.Utils;
import org.example.elements.hit.HitsDrop;

public class ShockCreature extends Round {   //痹生物
    private final int side;
    private int hp = 10;
    private float xs = 0;
    private float ys = 0;
    private float rot = 0;
    private int cnt;
    private float ycos = 0;
    private float ysin = 0;
    private int yrot = 0;
    private final GameTask game;

    public ShockCreature(GameTask GAME, float X, float Y, int S, int R) {   //初始化
        super(X, Y, 15.5F);
        xySync();
        game = GAME;
        side = S;
        rot = R;
        xs = Utils.cos(R) * 2.F;
        ys = Utils.sin(R) * 2.F;
        id = game.addElement(this);
        game.unit[side].addShape(this);
        cnt = 0;
    }

    @Override
    public void step() {   //每帧逻辑
        if (game.team[1 - side].hitTestPoint(x, y)) {
            hp--;
            new HitsDrop(game, x, y, game.atk[side]);
            x = x - 4 * Utils.cos((int) rot);
            y = y - 4 * Utils.sin((int) rot);
            xySync();
        }
        updateFloatOffset();
        x = x + xs;
        y = y + ys;
        xySync();
        x = x + ycos;
        y = y + ysin;
        xySync();

        if (y < -600 || x > 1920 || x < 0) {
            kill();
            return;
        }
        if (y > 570) {
            y = 570;
            ys = -ys;
        }
        cnt++;
        if (cnt == 60) {
            cnt = 0;
            int targetRot = Math.round((float) Math.toDegrees(Math.atan2(game.core_y[1 - side] - y, game.core_x[1 - side] - x)));
            targetRot = (targetRot % 360 + 360) % 360;
            new GunBullet(game, x, y, side).setVecMult(Utils.cos(targetRot), Utils.sin(targetRot), 10).move();
        }
        if (hp <= 0) {
            new HitsDrop(game, x, y, game.atk[side]);
            kill();
        }
    }

    public void kill() {
        game.elements.remove(id);
        game.unit[side].removeShape(this);
    }

    private void updateFloatOffset() {
        yrot = yrot + 10;
        if (yrot > 360) {
            yrot = yrot - 360;
        }
        ysin = Utils.sin(yrot) * 6.F;
        ycos = Utils.cos(yrot) * 6.F;
    }
}