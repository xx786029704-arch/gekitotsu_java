package org.example.elements;

import org.example.Main;
import org.example.Round;

public class GekiBullet extends Bullet {   //击玉火箭弹
    private float speed = 5F;
    private float rotation;

    public GekiBullet(float X, float Y, int S, float rotation) {   //初始化
        super(X, Y, S);
        this.rotation = rotation;
        this.rot = rotation;
        this.gei_flg = 1;
        updateVelocity();
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y < -1200 || this.x > 2560 || this.x < -640) {
            kill();
            return;
        }
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.y > 570 || this.gei_flg == 2) {
            new ExplosionHit(this.x, this.y, this.side, new float[]{30F, 60F, 39F, 12F});
            kill();
            return;
        }
        if (this.speed < 25F) {
            this.speed = this.speed + 1F;
        }
        updateVelocity();
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
    }

    private void updateVelocity() {   //更新速度向量
        float rad = (float) Math.toRadians(this.rotation);
        this.xs = (float) Math.cos(rad) * this.speed;
        this.ys = (float) Math.sin(rad) * this.speed;
    }

    private static class ExplosionHit extends Round {   //爆炸范围判定
        private int frame;
        private final int side;
        private final int id;
        private final float[] baseRadii;

        public ExplosionHit(float X, float Y, int side, float[] baseRadii) {   //初始化
            super(X, Y, baseRadii[0]);
            this.side = side;
            this.baseRadii = baseRadii;
            this.id = Main.addElement(this);
            Main.atk[side].addShape(this);
        }

        @Override
        public void step() {   //扩散动画
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
