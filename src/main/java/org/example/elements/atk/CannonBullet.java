package org.example.elements.atk;

import org.example.Main;
import org.example.Round;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBombMult;

public class CannonBullet extends Bullet {   //炮玉炮弹
    private int hp = 3;

    public CannonBullet(float X, float Y, int S) {   //初始化
        super(X, Y, S);
        gravity = 0.08F;
        gei_flg = 1;
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y > 570) {
            this.hp = 0;
        }
        else if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            if (Main.wall[1 - this.side].hitTestPoint(this.x, this.y) || Main.shield[1 - this.side].hitTestPoint(this.x, this.y)) {
                this.hp = 0;
            }
            else {
                this.hp--;
            }
        }
        move();
        if (this.hp <= 0) {
            new HitsBombMult(this.x, this.y, this.side, 1.2F);
            kill();
        }
    }
}
