package org.example;

import org.example.elements.base;

public class ShapeBuilder {
    private final CompositeShape composite;

    private ShapeBuilder(float x, float y){
        composite = new CompositeShape(x,y);
    }

    private ShapeBuilder(CompositeShape shape){
        composite = shape;
    }

    public static ShapeBuilder start(float x, float y){
        return new ShapeBuilder(x,y);
    }

    public static ShapeBuilder into(CompositeShape shape) {
        return new ShapeBuilder(shape);
    }

    public ShapeBuilder circle(float x, float y, float r){
        composite.addShape(new Round(composite.x+x,composite.y+y,r));
        return this;
    }

    public ShapeBuilder polygon(float x, float y, float[][] vxy){
        float xx = composite.x+x;
        float yy = composite.y+y;
        composite.addShape(new Polygon(composite.x+x,composite.y+y,vxy));
        return this;
    }

    public ShapeBuilder rectangle(float x, float y, float w, float h){
        float xx = composite.x+x;
        float yy = composite.y+y;
        float[][] vxy = {{0,0},{w,0},{w,h},{0,h}};
        composite.addShape(new Polygon(xx,yy,vxy));
        return this;
    }

    public ShapeBuilder roundedRectangle(float x, float y, float w, float h, float r){
        if (r <= 0) return rectangle(x,y,w,h);
        r = Math.min(r, Math.min(w,h) * 0.5f);
        float innerW = w - 2*r;
        float innerH = h - 2*r;
        rectangle(x + r, y, innerW, h);
        rectangle(x, y + r, w, innerH);
        circle(x + r, y + r, r);
        circle(x + w - r, y + r, r);
        circle(x + r, y + h - r, r);
        circle(x + w - r, y + h - r, r);
        return this;
    }

    public ShapeBuilder shape(Shape s){
        composite.addShape(s);
        return this;
    }

    public CompositeShape build(){
        return composite;
    }
}
