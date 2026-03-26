package org.example.elements.atk;

import org.example.Game;
import org.example.Round;
import org.example.Utils;
import org.example.elements.hit.HitsBombMult;

public class UkiBullet extends Round {   //浮玉子弹
    private final int side;
    private int hp = 1;
    private final float xs;
    private final float ys;
    private int yrot = 0;
    private float ysin = 0;
    private float ycos = 0;
    protected final Game game;

    public UkiBullet(Game game, float X, float Y, int S, float _xs, float _ys) {   //初始化
        super(X, Y, 15.5F);
        this.game = game;
        xySync();
        this.side = S;
        this.xs = _xs;
        this.ys = _ys;
        this.id = this.game.addElement(this);
        this.game.unit[side].addShape(this);
    }

    @Override
    public void step() {   //每帧逻辑
        updateFloatOffset();
        if (this.y > 570) {
            this.hp = 0;
        }
        else if (this.game.team[1 - this.side].hitTestPoint(this.x, this.y)) {
            if (this.game.fort[1 - this.side].hitTestPoint(this.x, this.y) || this.game.shield[1 - this.side].hitTestPoint(this.x, this.y)) {
                this.hp = 0;
            }
            else {
                this.hp--;
            }
        }
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        xySync();
        this.x = this.x + this.ycos;
        this.y = this.y + this.ysin;
        xySync();
        if (this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (this.hp <= 0) {
            new HitsBombMult(this.game, this.x, this.y, this.side, 1.2F);
            kill();
        }
    }

    public void kill() {
        this.game.elements.remove(id);
        this.game.unit[this.side].removeShape(this);
    }

    private void updateFloatOffset() {
        this.yrot = this.yrot + 10;
        if (this.yrot > 360) {
            this.yrot = this.yrot - 360;
        }
        this.ysin = Utils.sin(yrot);
        this.ycos = Utils.cos(yrot);
    }
}
