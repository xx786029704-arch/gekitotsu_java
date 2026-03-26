package org.example.elements.atk;

import org.example.Game;
import org.example.Round;
import org.example.Utils;
import org.example.elements.hit.HitsBombMult;
import org.example.elements.hit.HitsDrop;

public class ShockCreature extends Round {   //痹生物
    private final int side;
    private int hp = 10;
    private float xs = 0;
    private float ys = 0;
    private float rot = 0;
    private int cnt;
    private float ycos = 0;
    private float ysin = 0;
    private int yrot = 0;
    protected final Game game;

    public ShockCreature(Game game, float X, float Y, int S, int R) {   //初始化
        super(X, Y, 15.5F);
        this.game = game;
        xySync();
        this.side = S;
        this.rot = R;
        this.xs = Utils.cos(R) * 2.F;
        this.ys = Utils.sin(R) * 2.F;
        this.id = this.game.addElement(this);
        this.game.unit[side].addShape(this);
        this.cnt = 0;
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.game.team[1 - this.side].hitTestPoint(this.x, this.y)) {
            this.hp--;
            new HitsDrop(this.game, this.x, this.y, this.game.atk[side]);
            this.x = this.x - 4 * Utils.cos((int) this.rot);
            this.y = this.y - 4 * Utils.sin((int) this.rot);
            xySync();
        }
        updateFloatOffset();
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
        if (this.y > 570) {
            this.y = 570;
            this.ys = -this.ys;
        }
        this.cnt++;
        if (this.cnt == 60) {
            this.cnt = 0;
            int targetRot = Math.round((float) Math.toDegrees(Math.atan2(this.game.core_y[1 - this.side] - this.y, this.game.core_x[1 - this.side] - this.x)));
            targetRot = (targetRot % 360 + 360) % 360;
            new GunBullet(this.game, this.x, this.y, this.side).setVecMult(Utils.cos(targetRot), Utils.sin(targetRot), 10).move();
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

    private void updateFloatOffset() {
        this.yrot = this.yrot + 10;
        if (this.yrot > 360) {
            this.yrot = this.yrot - 360;
        }
        this.ysin = Utils.sin(yrot) * 6.F;
        this.ycos = Utils.cos(yrot) * 6.F;
    }
}