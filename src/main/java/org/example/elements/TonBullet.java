package org.example.elements;

import org.example.Main;
import org.example.Round;

public class TonBullet extends Bullet {   //弹玉弹球
    private final float power = 10F;
    private int hp = 3;
    private int bounceCount = 0;

    public TonBullet(float X, float Y, int S, float rotation) {   //初始化
        super(X, Y, S);
        this.rot = rotation;
        this.gei_flg = 1;
        this.r = this.r * 1.2F;
        float rad = (float) Math.toRadians(this.rot);
        this.xs = (float) Math.cos(rad) * power;
        this.ys = (float) Math.sin(rad) * power;
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y < -1200 || this.x > 2560 || this.x < -640) {
            spawnImpact();
            kill();
            return;
        }
        if (this.y > 570) {
            this.y = 570;
            this.ys = -this.ys;
            this.bounceCount++;
            if (this.bounceCount >= 5) {
                this.hp = 0;
            }
        }
        else if (Main.team[1 - this.side].hitTestPoint(this.x, this.y) || this.gei_flg == 2) {
            this.hp--;
            if (Main.wall[1 - this.side].hitTestPoint(this.x, this.y) || Main.shield[1 - this.side].hitTestPoint(this.x, this.y)) {
                this.hp = 0;
            }
        }
        if (this.hp <= 0) {
            spawnImpact();
            kill();
            return;
        }
        this.x = this.x + this.xs;
        this.y = this.y + this.ys;
        this.ys = this.ys + 0.32F;
    }

    private void spawnImpact() {   //触发冲击判定
        new ImpactHit(this.x, this.y, this.side, 31F);
    }

    private static class ImpactHit extends Round {   //冲击范围判定
        private final int side;
        private final int id;

        public ImpactHit(float X, float Y, int side, float radius) {   //初始化
            super(X, Y, radius);
            this.side = side;
            this.id = Main.addElement(this);
            Main.atk[side].addShape(this);
        }

        @Override
        public void step() {   //单帧判定
            Main.elements.remove(id);
            Main.atk[side].removeShape(this);
        }
    }
}
