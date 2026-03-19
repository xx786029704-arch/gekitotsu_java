package org.example.elements;

import org.example.Main;
import org.example.Round;

public class CannonBullet extends Bullet {
    private int hp = 3;

    public CannonBullet(float X, float Y, int S) {
        super(X, Y, S);
        gravity = 0.08F;
        gei_flg = 1;
    }

    @Override
    public void step() {
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

        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        this.ys = this.ys + this.gravity;

        if (this.hp <= 0) {
            new ExplosionHit(this.x, this.y, this.side, new float[]{16F, 60F, 39F, 12F}, 1.2F);
            kill();
        }
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
