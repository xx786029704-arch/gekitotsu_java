package org.example.elements.atk;

import org.example.GameTask;
import org.example.Round;
import org.example.elements.hit.HitsDropFrames;

public class ShaBullet extends Round {   //射玉瞄准射线
    protected final int side;
    private final int id;
    private final float xs;
    private final float ys;
    protected final GameTask game;

    public ShaBullet(GameTask GAME, float X, float Y, int S, float _xs, float _ys) {   //初始化
        super(X, Y, 15.5F);
        game = GAME;
        side = S;
        xs = _xs;
        ys = _ys;
        x = X;
        y = Y;
        id = game.addElement(this);
        xySync();
    }

    public void kill() {   //销毁
        game.elements.remove(id);
    }

    @Override
    public void step() {   //每帧逻辑
        boolean hit = false;
        for (int i = 0; i < 1000; i++) {
            x += xs;
            y += ys;
            if (y < -600 || x > 1920 || x < 0) {
                break;
            }
            if (game.fort[1 - side].hitTestPoint(x, y) || game.shield[1 - side].hitTestPoint(x, y) || y > 570) {
                hit = true;
                break;
            }
        }
        if (hit) {
            hit();
        }
        kill();
    }

    public void hit(){
        new HitsDropFrames(game, x, y, game.atk[side], 6, 1.3F);
    }
}
