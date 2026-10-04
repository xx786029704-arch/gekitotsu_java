package org.example.elements.units;

import org.example.GameTask;
import org.example.Utils;
import org.example.elements.Ball;
import org.example.elements.atk.SniperBullet;

public class SniperBall extends Ball {   //狙玉
    public SniperBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        speed = 100;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        float tRot = (float) Math.toDegrees(Math.atan2(game.core_y[1-side] - this.y, game.core_x[1-side] - this.x));
        tRot = Math.round(tRot);
        if (this.rot - 180 > tRot) {
            tRot += 360;
        }
        if (this.rot + 180 < tRot) {
            tRot -= 360;
        }
        if (this.rot > tRot) {
            this.rot -= 1;
        }
        if (this.rot < tRot) {
            this.rot += 1;
        }
        if (this.rot > 360) {
            this.rot -= 360;
        } else if (this.rot < 0) {
            this.rot += 360;
        }
        cos_rot = Utils.cos(rot);
        sin_rot = Utils.sin(rot);
        if (this.cnt == this.speed) {
            this.cnt = 0;
            float spawnX = this.x + this.cos_rot * 40;
            float spawnY = this.y + this.sin_rot * 40;
            new SniperBullet(game, spawnX, spawnY, this.side, cos_rot * 10F, sin_rot * 10F);
        }
    }
}
