package org.example.elements;

import org.example.Main;
import org.example.Round;

public class KakuBullet extends Bullet {   //核玉核弹
    private float speed = 1F;
    private int hp = 4;

    public KakuBullet(float X, float Y, int S, float rotation) {   //初始化
        super(X, Y, S);
        this.rot = rotation;
        this.gei_flg = 1;
        this.gravity = 0;
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y < -1200 || this.x > 2560 || this.x < -640) {
            kill();
            return;
        }
        if (Main.atk[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            this.hp--;
        }
        if (this.hp <= 0 || this.y > 570) {
            explode(1F);
            kill();
            return;
        }
        if (Main.wall[1 - this.side].hitTestPoint(this.x, this.y) || Main.shield[1 - this.side].hitTestPoint(this.x, this.y)) {
            explode(8F);
            kill();
            return;
        }
        if (this.speed < 3F) {
            this.speed = this.speed + 0.01F;
        }
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * this.speed;
        this.ys = (float) Math.sin(rad) * this.speed;
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
    }

    private void explode(float multiplier) {   //触发爆炸
        new ExplosionHit(this.x, this.y, this.side, new float[]{30F, 60F, 39F, 12F}, multiplier);
    }

    private static class ExplosionHit extends Round {   //爆炸范围判定
        private int frame;
        private final int side;
        private final int id;
        private final float[] baseRadii;
        private final float multiplier;

        public ExplosionHit(float X, float Y, int side, float[] baseRadii, float multiplier) {   //初始化
            super(X, Y, baseRadii[0] * multiplier);
            this.side = side;
            this.baseRadii = baseRadii;
            this.multiplier = multiplier;
            this.id = Main.addElement(this);
            Main.atk[side].addShape(this);
        }

        @Override
        public void step() {   //扩散动画
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
