package org.example.elements.atk;

import org.example.GameTask;
import org.example.Round;
import org.example.Utils;
import org.example.elements.hit.HitsDropFrames;
import org.example.elements.hit.HitsDrop;

public class WeakCreature extends Round {   //，，弱，，生物
    private final int side;
    private int hp = 3;
    private float xs = 0;
    private float ys = 0;
    private float xs2 = 0;
    private float rot = 0;
    private final GameTask game;

    public WeakCreature(GameTask GAME, float X, float Y, int S, int R) {   //初始化
        super(X, Y, 15.5F);
        xySync();
        game = GAME;
        side = S;
        rot = R;
        xs = Utils.cos(R) * 3.F;
        ys = Utils.sin(R) * 3.F;
        id = game.addElement(this);
        game.unit[side].addShape(this);
    }

    @Override
    public void step() {   //每帧逻辑
        if (game.team[1 - side].hitTestPoint(x, y)) {
            xs2 = -4 + 8 * side;
            hp--;
            new HitsDropFrames(game, x, y, game.atk[side], 1, 1.2F);
        }
        int targetRot = Math.round((float) Math.toDegrees(Math.atan2(game.core_y[1 - side] - y, game.core_x[1 - side] - x)));
        targetRot = (targetRot % 360 + 360) % 360;
        rot = rot * .875F + targetRot * .125F;
        xs = Utils.cosF(rot) * 3.F;
        ys = Utils.sinF(rot) * 3.F;
        x = x + xs + xs2;
        y = y + ys;
        xySync();
        xs2 *= .9F;
        if (y < -600 || x > 1920 || x < 0) {
            kill();
            return;
        }
        if (y > 570) {
            y = 570;
            ys = -ys;
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

}