package org.example.elements.atk;

import org.example.GameTask;
import org.example.Round;
import org.example.Utils;
import org.example.elements.hit.HitsDrop;

public class SolidCreature extends Round {   //固生物
    private final int side;
    private int hp = 20;
    private float xs = 0;
    private float ys = 0;
    private float rot = 0;
    private final GameTask game;

    public SolidCreature(GameTask GAME, float X, float Y, int S, int R) {   //初始化
        super(X, Y, 15.5F);
        xySync();
        game = GAME;
        side = S;
        rot = R;
        xs = Utils.cos(R) * 2.F;
        ys = Utils.sin(R) * 2.F;
        id = game.addElement(this);
        game.unit[side].addShape(this);
    }

    @Override
    public void step() {   //每帧逻辑
        if (game.team[1 - side].hitTestPoint(x, y)) {
            hp--;
            new HitsDrop(game, x, y, game.atk[side]);
            x = x - Utils.cos((int) rot);
            y = y - Utils.sin((int) rot);
            xySync();
        }
        x = x + xs;
        y = y + ys;
        xySync();
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