package org.example.elements.atk;

import org.example.Game;
import org.example.Round;
import org.example.elements.hit.HitsDropFrames;

public class ShaBullet extends Round {   //射玉瞄准射线
    protected final int side;
    private final int id;
    private final float xs;
    private final float ys;
    protected final Game game;

    public ShaBullet(Game game, float X, float Y, int S, float _xs, float _ys) {   //初始化
        super(X, Y, 15.5F);
        this.game=game;
        this.side = S;
        this.xs = _xs;
        this.ys = _ys;
        this.x = X;
        this.y = Y;
        this.id = this.game.addElement(this);
        xySync();
    }

    public void kill() {   //销毁
        this.game.elements.remove(id);
    }

    @Override
    public void step() {   //每帧逻辑
        boolean hit = false;
        for (int i = 0; i < 1000; i++) {
            this.x += this.xs;
            this.y += this.ys;
            if (this.y < -600 || this.x > 1920 || this.x < 0) {
                break;
            }
            if (this.game.fort[1 - this.side].hitTestPoint(this.x, this.y) || this.game.shield[1 - this.side].hitTestPoint(this.x, this.y) || this.y > 570) {
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
        new HitsDropFrames(this.game, this.x, this.y, this.game.atk[side], 6, 1.3F);
    }
}
