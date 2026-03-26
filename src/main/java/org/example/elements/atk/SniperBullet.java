package org.example.elements.atk;

import org.example.Game;
import org.example.Round;
import org.example.elements.hit.HitsDrop;
import org.example.elements.hit.HitsDropFrames;

public class SniperBullet extends ShaBullet {   //狙玉内部射线

    public SniperBullet(Game game, float X, float Y, int S, float _xs, float _ys) {   //初始化
        super(game, X, Y, S, _xs, _ys);
    }

    public void hit() {
        new HitsDropFrames(this.game, this.x, this.y, this.game.atk[side], 3, 1.3F);
    }
}
