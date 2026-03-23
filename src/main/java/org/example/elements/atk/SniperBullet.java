package org.example.elements.atk;

import org.example.Main;
import org.example.Round;
import org.example.elements.hit.HitsDrop;
import org.example.elements.hit.HitsDropFrames;

public class SniperBullet extends ShaBullet {   //狙玉内部射线

    public SniperBullet(float X, float Y, int S, float _xs, float _ys) {   //初始化
        super(X, Y, S, _xs, _ys);
    }

    public void hit(){
        new HitsDropFrames(this.x, this.y, Main.atk[side], 3, 1.3F);
    }
}
