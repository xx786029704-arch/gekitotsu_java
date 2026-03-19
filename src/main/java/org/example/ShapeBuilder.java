package org.example;

public class ShapeBuilder {     //复合图形建造者
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
    }   //改为给指定复合图形增加图形

    public ShapeBuilder circle(float x, float y, float r){      //增加一个圆
        composite.addShape(new Round(composite.x+x,composite.y+y,r));
        return this;
    }

    public ShapeBuilder polygon(float x, float y, float[][] vxy){       //增加一个多边形
        float xx = composite.x+x;
        float yy = composite.y+y;
        composite.addShape(new Polygon(composite.x+x,composite.y+y,vxy));
        return this;
    }

    public ShapeBuilder rectangle(float x, float y, float w, float h){      //增加一个矩形
        float xx = composite.x+x;
        float yy = composite.y+y;
        float[][] vxy = {{0,0},{w,0},{w,h},{0,h}};
        composite.addShape(new Polygon(xx,yy,vxy));
        return this;
    }

    public ShapeBuilder roundedRectangle(float x, float y, float w, float h, float r){      //增加一个圆角矩形
        if (r <= 0) return rectangle(x,y,w,h);
        r = Math.min(r, Math.min(w,h) * 0.5f);
        float innerW = w - 2 * r;
        float innerH = h - 2 * r;
        rectangle(x + r, y, innerW, h);
        rectangle(x, y + r, w, innerH);
        circle(x + r, y + r, r);
        circle(x + w - r, y + r, r);
        circle(x + r, y + h - r, r);
        circle(x + w - r, y + h - r, r);
        return this;
    }

    public ShapeBuilder sector(float x, float y, float r, float angle, float direction){      //增加一个扇形
        composite.addShape(new Sector(composite.x+x,composite.y+y, r, angle, direction));
        return this;
    }

    public ShapeBuilder shape(Shape s){     //增加指定图形
        composite.addShape(s);
        return this;
    }

    public CompositeShape build(){
        return composite;
    }
}
