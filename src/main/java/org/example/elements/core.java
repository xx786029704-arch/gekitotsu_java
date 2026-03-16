package org.example.elements;

import org.example.CompositeShape;
import org.example.Main;
import org.example.ShapeBuilder;

public class core extends CompositeShape {
    public int side;
    public float unit_x;
    public float unit_y;

    public core(float X, float Y, int S) {
        super(X, Y);
        this.side = S;
        unit_x = X - Main.bases[side].x;
        unit_y = Y - Main.bases[side].y;
        float[][] vxy1 = {{-28.7F,-35.8F},{-35.8F,-28.7F},{28.7F,35.8F},{35.8F,28.7F}};
        float[][] vxy2 = {{-28.7F,35.8F},{-35.8F,28.7F},{28.7F,-35.8F},{35.8F,-28.7F}};
        ShapeBuilder.into(this)
                .circle(-40,-40,12)
                .circle(-40,40,12)
                .circle(40,40,12)
                .circle(40,-40,12)
                .circle(0,-0,32)
                .polygon(0,0,vxy1)
                .polygon(0,0,vxy2);
        Main.elements.add(this);
        Main.wall[side].addShape(this);
    }

    @Override
    public void step(){
        this.move(Main.bases[side].x + this.unit_x - x,Main.bases[side].y + this.unit_y - y);
    }
}
