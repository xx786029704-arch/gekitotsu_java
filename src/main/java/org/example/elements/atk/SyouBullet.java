package org.example.elements.atk;

import org.example.*;
import org.example.Rectangle;
import org.example.elements.hit.HitsDrop;

import java.awt.*;

public class SyouBullet extends Rectangle {   //障玉障壁
    private final int side;
    private final int id;
    private int hp = 10;
    private int cnt = -7;
    private float power = 20F;
    private final float cosRot;
    private final float sinRot;
    private boolean active = false;
    protected final Game game;

    public SyouBullet(Game game, float X, float Y, int S, float cos_rot, float sin_rot) {   //初始化
        super(X, Y, -14, -14, 28, 28);
        this.game = game;
        side = S;
        cosRot = cos_rot;
        sinRot = sin_rot;
        id = this.game.addElement(this);
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.game.team[1 - side].hitTestPoint(x, y)) {
            hp--;
            if (this.game.fort[1 - side].hitTestPoint(x, y) || this.game.shield[1 - side].hitTestPoint(x, y)) {
                hp = 0;
            }
        }
        if (y > 570 || y < -600 || x > 1920 || x < 0 || hp <= 0 || cnt >= 159) {
            if (active) {
                new HitsDrop(this.game, x, y, this.game.atk[side]);
            }
            kill();
            return;
        }
        if (power > 0) {
            power--;
            x += cosRot * power;
            y += sinRot * power;
            xySync();
        } else {
            cnt++;
            if (cnt == 0){
                activate();
            }
        }
    }

    private void activate() {
        if (!active) {
            active = true;
            this.game.unit[side].addShape(this);
        }
    }

    private void kill() {
        this.game.elements.remove(id);
        if (active) {
            this.game.unit[side].removeShape(this);
        }
    }

    @Override
    public void draw(Graphics2D g2d) {   //绘制
        if (!active){
            g2d.setColor(Color.DARK_GRAY);
            super.draw(g2d);
            g2d.setColor(Color.WHITE);
            return;
        }
        super.draw(g2d);
    }
}
