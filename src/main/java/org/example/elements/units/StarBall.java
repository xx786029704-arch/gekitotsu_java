package org.example.elements.units;

import org.example.Game;
import org.example.Utils;
import org.example.elements.Ball;
import org.example.elements.hit.StarPrepareLaser;

public class StarBall extends Ball {   //星玉
    public boolean shooting;
    public StarBall(Game game, float X, float Y, int R, int S, int TYPE) {   //初始化
        super(game, X, Y, R, S, TYPE);
        shooting = false;
        speed = 250;
        hp = 10;
        max_hp = 10;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.jump_flg == 1) {
            this.shooting = false;
        } else {
            if (this.cnt == this.speed + 1) {
                this.shooting = true;
                new StarPrepareLaser(this.game, this.x + 28.F * Utils.cos((int) this.rot), this.y + 28.F * Utils.sin((int) this.rot), this.rot, this.side, this.id);
            }
            if (this.cnt >= this.speed + 31) {
                this.cnt = 0;
                this.shooting = false;
            }
        }
    }

    @Override
    public void hurt(boolean is_crash) {
        this.shooting = false;
    }
}