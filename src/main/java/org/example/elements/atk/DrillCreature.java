package org.example.elements.atk;

import org.example.Main;
import org.example.Round;
import org.example.Utils;
import org.example.elements.hit.HitsDrop;

public class DrillCreature extends Round {   //尖生物
    private final int side;
    private int hp = 7;
    private float xuv = 0;
    private float yuv = 0;
    private float speed = 0;

    public DrillCreature(float X, float Y, int S, int R) {   //初始化
        super(X, Y, 15.5F);
        xySync();
        this.side = S;
        this.xuv = Utils.cos(R) ;
        this.yuv = Utils.sin(R);
        this.id = Main.addElement(this);
        Main.unit[side].addShape(this);
    }

    @Override
    public void step() {   //每帧逻辑
        if (Main.team[1 - this.side].hitTestPoint(this.x, this.y)) {
            this.speed = -5.F;
            this.hp--;
            new HitsDrop(this.x, this.y, Main.atk[side]);
        }
        if (this.speed < 25.F) {
            this.speed += 1.F;
        }
        this.x = this.x + this.xuv*this.speed;
        this.y = this.y + this.yuv*this.speed;
        xySync();
        if (this.y < -600 || this.x > 1920 || this.x < 0) {
            kill();
            return;
        }
        if (this.y > 570 || this.hp <= 0) {
            new HitsDrop(this.x, this.y, Main.atk[side]);
            kill();
        }
    }

    public void kill() {
        Main.elements.remove(id);
        Main.unit[this.side].removeShape(this);
    }

}