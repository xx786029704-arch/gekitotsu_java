package org.example.elements;

import org.example.Main;
import org.example.Round;

public class MissileBullet extends Bullet {
    private int cnt = 0;
    private float rotation;
    private float speed = 4F;

    public MissileBullet(float X, float Y, int S, float rotation) {
        super(X, Y, S);
        this.rotation = rotation;
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
            new ExplosionHit(this.x, this.y, this.side, new float[]{30F, 60F, 39F, 12F}, 1F);
            kill();
            return;
        }
        Core target = Main.cores[1 - this.side];
        if (target != null) {
            float targetRot = (float) Math.toDegrees(Math.atan2(target.y - this.y, target.x - this.x));
            if (this.rotation - 180 > targetRot) {
                targetRot += 360;
            }
            if (this.rotation + 180 < targetRot) {
                targetRot -= 360;
            }
            if (this.speed < 15F) {
                this.speed += 0.3F;
            }
            if (this.speed > 14F) {
                this.rotation = this.rotation + (targetRot - this.rotation) / 8F;
            }
        }
        updateVelocity();
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
    }

    private void updateVelocity() {
        float rad = (float) Math.toRadians(this.rotation);
        this.xs = (float) Math.cos(rad) * this.speed;
        this.ys = (float) Math.sin(rad) * this.speed;
    }

    private static class ExplosionHit extends Round {
        private int frame;
        private final int side;
        private final int id;
        private final float[] baseRadii;
        private final float multiplier;

        public ExplosionHit(float X, float Y, int side, float[] baseRadii, float multiplier) {
            super(X, Y, baseRadii[0] * multiplier);
            this.side = side;
            this.baseRadii = baseRadii;
            this.multiplier = multiplier;
            this.id = Main.addElement(this);
            Main.atk[side].addShape(this);
        }

        @Override
        public void step() {
            frame++;
            if (frame < baseRadii.length) {
                this.r = baseRadii[frame] * multiplier;
            }
            if (frame >= baseRadii.length) {
                Main.elements.remove(id);
                Main.atk[side].removeShape(this);
            }
        }
    }
}
