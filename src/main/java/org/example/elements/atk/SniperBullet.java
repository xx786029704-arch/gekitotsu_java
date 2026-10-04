package org.example.elements.atk;

import org.example.GameTask;
import org.example.elements.hit.HitsDropFrames;

public class SniperBullet extends ShaBullet {   //狙玉内部射线

    public SniperBullet(GameTask GAME, float X, float Y, int S, float _xs, float _ys) {   //初始化
        super(GAME, X, Y, S, _xs, _ys);
    }

    public void hit(){
        new HitsDropFrames(game, x, y, game.atk[side], 3, 1.3F);
    }
}
