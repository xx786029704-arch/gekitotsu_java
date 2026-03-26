package org.example.elements.hit;

import org.example.*;
import java.awt.*;
import java.awt.geom.Path2D;

public class HitsKen extends EllipticalSector {   //剑玉剑气
    private final int side;
    private int frame;
    private final int user;
    private final boolean flipped;
    private final float cos_rot;
    private final float sin_rot;
    @Deprecated
    private final float rot_radius; //仅渲染使用，后期可删除
    protected final Game game;

    public HitsKen(Game game, float X, float Y, int R, int S, int USER, float _cos_rot, float _sin_rot) {
        super(X, Y, 61, 74, 273, 1.1296296F, 0.052335956243F, -0.998629534754F, 0.79863551F);
        this.game = game;
        xySync();
        frame = 0;
        user = USER;
        side = S;
        cos_rot = _cos_rot;
        sin_rot = _sin_rot;
        rot_radius = R * 0.017453292519943295F;
        flipped = R >= 90 + side && R <= 270 + side;
        id = this.game.addElement(this);
        this.game.atk[side].addShape(this);
    }

    public void kill() {
        this.game.elements.remove(id);
        this.game.atk[side].removeShape(this);
    }

    /*
        可能的优化：可以不使用update而是直接硬编码dirX dirY cosHalfAngle，但有可能编译器已经帮我们干了，目前不确定需不需要手动干
     */

    @Override
    public void step(){
        if (this.game.elements.containsKey(user)){
            this.x = this.game.elements.get(user).x;
            this.y = this.game.elements.get(user).y;
        }
        switch (frame){
            case 0:{
                a = 124 * 0.017453292519943295F;
                dir = 298 * 0.017453292519943295F;
                update();
                break;
            }
            case 1:{
                a = 161 * 0.017453292519943295F;
                dir = 316.5F * 0.017453292519943295F;
                update();
                break;
            }
            case 2:{
                a = 87 * 0.017453292519943295F;
                dir = 353.5F * 0.017453292519943295F;
                update();
                break;
            }
            case 3:{
                a = 37 * 0.017453292519943295F;
                dir = 18.5F * 0.017453292519943295F;
                update();
                break;
            }
            case 4:{
                kill();
                break;
            }
        }
        frame++;
    }

    @Override
    public boolean hitTestPoint(float X, float Y){
        float dx = ((X - x) * cos_rot + (Y - y) * sin_rot);
        float dy = ((Y - y) * cos_rot - (X - x) * sin_rot);
        dy *= flatness;
        if (flipped){
            dy = -dy;
        }
        if (dx > r || dy > r || dx < -r || dy < -r){
            return false;
        }
        float dist2 = dx * dx + dy * dy;
        if (dist2 > r * r)
            return false;
        float dot = dx * dirX + dy * dirY;
        float cos2 = cosHalfAngle * cosHalfAngle;
        return dot > 0 && dot * dot >= dist2 * cos2;
    }

    @Override
    public void draw(Graphics2D g2d){
        if (flipped) {
            g2d.translate(0, y);
            g2d.scale(1, -1);
            g2d.translate(0, -y);
        }
        g2d.rotate(flipped ? -rot_radius : rot_radius, (int) x, (int) y);
        super.draw(g2d);
        g2d.rotate(flipped ? rot_radius : -rot_radius, (int) x, (int) y);
        if (flipped) {
            g2d.translate(0, y);
            g2d.scale(1, -1);
            g2d.translate(0, -y);
        }
    }
}
