package org.example.elements.atk;

import org.example.GameTask;
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
    private final GameTask game;

    public UkiBullet(GameTask GAME, float X, float Y, int S, float _xs, float _ys) {   //初始化
        super(X, Y, 15.5F);
        xySync();
        game = GAME;
        this.side = S;
        this.xs = _xs;
        this.ys = _ys;
        this.id = game.addElement(this);
        game.unit[side].addShape(this);
    }

    @Override
    public void step() {   //每帧逻辑
        updateFloatOffset();
        if (this.y > 570) {
            this.hp = 0;
        }
        else if (game.team[1 - this.side].hitTestPoint(this.x, this.y)) {
            if (game.fort[1 - this.side].hitTestPoint(this.x, this.y) || game.shield[1 - this.side].hitTestPoint(this.x, this.y)) {
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
            new HitsBombMult(game, this.x, this.y, this.side, 1.2F);
            kill();
        }
    }

    public void kill() {
        game.elements.remove(id);
        game.unit[this.side].removeShape(this);
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
