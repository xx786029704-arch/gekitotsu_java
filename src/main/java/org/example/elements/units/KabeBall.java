package org.example.elements.units;

import org.example.Game;
import org.example.elements.Ball;
import org.example.elements.hit.HitsKabe;

public class KabeBall extends Ball {   //壁玉
    public boolean shooting;
    private final int unit_x;
    public KabeBall(Game game, float X, float Y, int R, int S, int TYPE) {   //初始化
        super(game, X, Y, R, S, TYPE);
        shooting = false;
        speed = 60;
        hp = 20;
        max_hp = 20;
        unit_x = (int)X;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.jump_flg==1){
            this.shooting=false;
        }
        else {
            if (this.cnt == this.speed) {
                this.shooting = true;
                new HitsKabe(this.game, this.x + 20 * cos_rot, this.y + 20 * sin_rot, this.rot, this.side, this.id);
            }
            if (this.cnt > this.speed + 300) {
                this.cnt = 0;
                this.shooting = false;
            }
        }
    }

    @Override
    public void hurt(boolean is_crash) {
        this.shooting = false;
        this.cnt = -this.unit_x % this.speed;
    }
}