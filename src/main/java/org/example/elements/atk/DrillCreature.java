package org.example.elements.atk;

import org.example.GameTask;
import org.example.Round;
import org.example.Utils;
import org.example.elements.hit.HitsDrop;

public class DrillCreature extends Round {   //尖生物
    private final int side;
    private int hp = 7;
    private float xuv = 0;
    private float yuv = 0;
    private float speed = 0;
    private final GameTask game;

    public DrillCreature(GameTask GAME, float X, float Y, int S, int R) {   //初始化
        super(X, Y, 15.5F);
        xySync();
        game = GAME;
        side = S;
        xuv = Utils.cos(R) ;
        yuv = Utils.sin(R);
        id = game.addElement(this);
        game.unit[side].addShape(this);
    }

    @Override
    public void step() {   //每帧逻辑
        if (game.team[1 - side].hitTestPoint(x, y)) {
            speed = -5.F;
            hp--;
            new HitsDrop(game, x, y, game.atk[side]);
        }
        if (speed < 25.F) {
            speed += 1.F;
        }
        x = x + xuv*speed;
        y = y + yuv*speed;
        xySync();
        if (y < -600 || x > 1920 || x < 0) {
            kill();
            return;
        }
        if (y > 570 || hp <= 0) {
            new HitsDrop(game, x, y, game.atk[side]);
            kill();
        }
    }

    public void kill() {
        game.elements.remove(id);
        game.unit[side].removeShape(this);
    }

}