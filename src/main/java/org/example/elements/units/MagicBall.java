package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.hit.HitsKabe;
import org.example.elements.hit.MagicLaser;
import org.example.Utils;

public class MagicBall extends Ball {   //壁玉
    public boolean shooting;
    public MagicBall(float X, float Y, float R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        shooting = false;
        speed = 150;
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
                new MagicLaser(this.x + 38.F * Utils.cos((int) this.rot), this.y + 38.F * Utils.sin((int) this.rot), this.rot, this.side, this.id);
            }
            if (this.cnt >= this.speed + 38) {
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
