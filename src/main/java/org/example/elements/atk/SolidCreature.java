package org.example.elements.atk;

import org.example.Game;
import org.example.Round;
import org.example.Utils;
import org.example.elements.hit.HitsDrop;

public class SolidCreature extends Round {   //固生物
    private final int side;
    private int hp = 20;
    private float xs = 0;
    private float ys = 0;
    private float rot = 0;
    protected final Game game;

    public SolidCreature(Game game, float X, float Y, int S, int R) {   //初始化
        super(X, Y, 15.5F);
        this.game = game;
        xySync();
        this.side = S;
        this.rot = R;
        this.xs = Utils.cos(R) * 2.F;
        this.ys = Utils.sin(R) * 2.F;
        this.id = this.game.addElement(this);
        this.game.unit[side].addShape(this);
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.game.team[1 - this.side].hitTestPoint(this.x, this.y)) {
            this.hp--;
            new HitsDrop(this.game, this.x, this.y, this.game.atk[side]);
            this.x = this.x - Utils.cos((int) this.rot);
            this.y = this.y - Utils.sin((int) this.rot);
            xySync();
        }
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        xySync();
        if (this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (this.y > 570) {
            this.y = 570;
            this.ys = -this.ys;
        }
        if (this.hp <= 0) {
            new HitsDrop(this.game, this.x, this.y, this.game.atk[side]);
            kill();
        }
    }

    public void kill() {
        this.game.elements.remove(id);
        this.game.unit[this.side].removeShape(this);
    }
}