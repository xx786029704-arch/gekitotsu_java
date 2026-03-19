package org.example.elements.atk;

import org.example.Main;
import org.example.Round;
import org.example.elements.Bullet;
import org.example.elements.Core;
import org.example.elements.hit.HitsBomb;

public class MissileBullet extends Bullet {
    private int cnt = 0;
    private float speed = 4F;

    public MissileBullet(float X, float Y, int S, float rotation) {
        super(X, Y, S);
        this.rot = rotation;
        this.gei_flg = 1;
        updateVelocity();
    }

    @Override
    public void step() {
        cnt++;
        if (this.y < -1200 || this.x > 2560 || this.x < -640) {
            kill();
            return;
        }
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.y > 570 || this.gei_flg == 2 || cnt > 200) {
            new HitsBomb(this.x, this.y, this.side);
            kill();
            return;
        }
        if (Main.cores[1-this.side] != null) {
            float targetRot = (float) Math.toDegrees(Math.atan2(Main.cores[1-this.side].y - this.y, Main.cores[1-this.side].x - this.x));
            targetRot = (targetRot % 360 + 360) % 360;
            if (this.speed < 15F) {
                this.speed += 0.3F;
            }
            if (this.speed > 14F) {
                this.rot = this.rot + (targetRot - this.rot) / 8F;
            }
        }
        updateVelocity();
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
    }

    private void updateVelocity() {
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * this.speed;
        this.ys = (float) Math.sin(rad) * this.speed;
    }
}
