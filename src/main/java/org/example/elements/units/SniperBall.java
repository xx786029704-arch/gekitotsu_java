package org.example.elements.units;

import org.example.Main;
import org.example.elements.Ball;
import org.example.elements.Core;
import org.example.elements.atk.SniperBullet;

public class SniperBall extends Ball {   //狙玉
    public SniperBall(float X, float Y, float R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        speed = 100;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        Core target = Main.cores[1 - this.side];
        if (target != null) {
            float tRot = (float) Math.toDegrees(Math.atan2(target.y - this.y, target.x - this.x));
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
            this.rot_radius = this.rot * 0.01745329252F;
            this.cos_rot = (float) Math.cos(this.rot_radius);
            this.sin_rot = (float) Math.sin(this.rot_radius);
        }
        if (this.cnt == this.speed) {
            this.cnt = 0;
            float spawnX = this.x + this.cos_rot * 40;
            float spawnY = this.y + this.sin_rot * 40;
            new SniperBullet(spawnX, spawnY, this.side, this.rot);
        }
    }
}
