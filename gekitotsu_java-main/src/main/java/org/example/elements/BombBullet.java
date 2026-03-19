package org.example.elements;

import org.example.Main;
import org.example.Round;

public class BombBullet extends Bullet {
    public BombBullet(float X, float Y, int S) {
        super(X, Y, S);
        gei_flg = 1;
    }

    @Override
    public void step() {
        if (this.y > 570 || Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            new ExplosionHit(this.x, this.y, this.side, new float[]{30F, 60F, 39F, 12F});
            kill();
            return;
        }
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        this.ys = this.ys + this.gravity;
    }

    private static class ExplosionHit extends Round {
        private int frame;
        private final int side;
        private final int id;
        private final float[] baseRadii;

        public ExplosionHit(float X, float Y, int side, float[] baseRadii) {
            super(X, Y, baseRadii[0]);
            this.side = side;
            this.baseRadii = baseRadii;
            this.id = Main.addElement(this);
            Main.atk[side].addShape(this);
        }

        @Override
        public void step() {
            frame++;
            if (frame < baseRadii.length) {
                this.r = baseRadii[frame];
            }
            if (frame >= baseRadii.length) {
                Main.elements.remove(id);
                Main.atk[side].removeShape(this);
            }
        }
    }
}
