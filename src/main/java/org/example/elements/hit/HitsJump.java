package org.example.elements.hit;

import org.example.CompositeShape;
import org.example.Main;
import org.example.ShapeBuilder;

//继承CompositeShape仅用作渲染，最终成品可以移除相关代码
public class HitsJump extends CompositeShape {
    private final CompositeShape parent;

    public HitsJump(float X, float Y, CompositeShape s) {
        super(X, Y);
        ShapeBuilder.into(this)     //一个圆角矩形
                .roundedRectangle(-17.15F,-17.5F,34.35F,35F,4F);
        id = Main.addElement(this);
        parent = s;
        s.addShape(this);
    }

    @Override
    public void step(){
        Main.elements.remove(id);
        parent.removeShape(this);
    }

    //重写 HitTestPoint，只需要一次AABB检测而非原先的6次，分成9个区域进行检测，5个区域可直接通过，剩下4个区域拼成一个圆形再检测，极大程度节省性能
    @Override
    public boolean hitTestPoint(float X, float Y){
        float dx = X - x + 0.3F;
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
        return dx * dx + dy * dy <= 16F;
    }
}
