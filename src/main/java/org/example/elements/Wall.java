package org.example.elements;

import org.example.CompositeShape;
import org.example.Main;
import org.example.Shape;
import org.example.ShapeBuilder;
import org.example.elements.hit.HitsKen;

import java.awt.*;

//继承CompositeShape仅用作渲染，最终成品可以移除相关代码
public class Wall extends CompositeShape {  //要塞壁类
    public int side;
    public int type;
    public int hp = 35;
    public int max_hp = 35;
    public int breaking = 0;

    public Wall(float X, float Y, int S, int TYPE) {
        super(X, Y);
        this.side = S;
        this.type = TYPE;
        ShapeBuilder.into(this)     //一个圆角矩形
                .roundedRectangle(-16.85F,-17.5F,34.35F,35F,4F);
        id = Main.addElement(this);
        Main.wall[side].addShape(this);
    }

    public void kill() {
        Main.wall[side].removeShape(this);
        Main.elements.remove(id);
    }

    @Override
    public void step(){
        this.move(Main.bases[side].base_move_x,Main.bases[side].base_move_y);
        stepEx();
        if (breaking > 0 || Main.hp0_flg[this.side] > 0) {
            breaking++;
            if (breaking > 4 || Main.hp0_flg[this.side] > 0) {
                kill();
                return;
            }
        }
        else if (Main.atk[1-side].hitTestPoint(this.x - 6, this.y - 6) ||
                Main.atk[1-side].hitTestPoint(this.x + 6, this.y - 6) ||
                Main.atk[1-side].hitTestPoint(this.x - 6, this.y + 6) ||
                Main.atk[1-side].hitTestPoint(this.x + 6, this.y + 6)) {
            hp--;
            if (this.hp <= 0) {
                breaking++;
            }
        }
        if (hp < max_hp && Main.repair[side].hitTestPoint(this.x, this.y)) {
            hp ++;
        }
    }

    public void stepEx(){   //额外行为
    }

    @Override   //重写 HitTestPoint，只需要一次AABB检测而非原先的6次，分成9个区域进行检测，5个区域可直接通过，剩下4个区域拼成一个圆形再检测，极大程度节省性能
    public boolean hitTestPoint(float X, float Y){
        float dx = X - x;
        float dy = Y - y;
        if (dx < -16.85F || dx > 17.5F || dy < -17.5F || dy > 17.5F){
            return false;
        }
        if (dx > -12.85F){
            if (dx > 13.5F){
                dx -= 13.5F;
            } else {
                return true;
            }
        } else {
            dx += 12.85F;
        }
        if (dy > -13.5F){
            if (dy > 13.5F){
                dy -= 13.5F;
            } else {
                return true;
            }
        } else {
            dy += 13.5F;
        }
        return dx * dx + dy * dy <= 16F;     //此值<=12.27时，四段突可以生效
    }
}
