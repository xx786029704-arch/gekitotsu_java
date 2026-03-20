package org.example.elements.atk;

import org.example.Main;
import org.example.Round;
public class ShaBullet extends Round {   //射玉瞄准射线
    private final int side;
    private final int id;
    private float xs;
    private float ys;

    public ShaBullet(float X, float Y, int S, float rotation) {   //初始化
        super(X, Y, 15.5F);
        this.side = S;
        float rad = (float) Math.toRadians(rotation);
        this.xs = (float) Math.cos(rad) * 10F;
        this.ys = (float) Math.sin(rad) * 10F;
        this.x = X;
        this.y = Y;
        this.id = Main.addElement(this);
    }

    public void kill() {   //销毁
        Main.elements.remove(id);
    }

    @Override
    public void step() {   //每帧逻辑
        boolean hit = false;
        for (int i = 0; i < 1000; i++) {
            this.x += this.xs;
            this.y += this.ys;
            if (this.y < -600 || this.x > 1920 || this.x < 0) {
                hit = false;
                break;
            }
            if (Main.unit[1 - this.side].hitTestPoint(this.x, this.y) || Main.wall[1 - this.side].hitTestPoint(this.x, this.y) || Main.shield[1 - this.side].hitTestPoint(this.x, this.y) || this.y > 570) {
                hit = true;
                break;
            }
        }
        if (hit) {
            new ImpactHit(this.x, this.y, this.side, 6, 1.3F);
        }
        kill();
    }

    private static class ImpactHit extends Round {   //冲击范围判定
        private int frames;
        private final int side;
        private final int id;

        public ImpactHit(float X, float Y, int side, int frames, float scale) {   //初始化
            super(X, Y, 15.5F * scale);
            this.side = side;
            this.frames = frames;
            this.id = Main.addElement(this);
            Main.atk[side].addShape(this);
        }

        @Override
        public void step() {   //单帧消散
            frames--;
            if (frames <= 0) {
                Main.elements.remove(id);
                Main.atk[side].removeShape(this);
            }
        }
    }
}
