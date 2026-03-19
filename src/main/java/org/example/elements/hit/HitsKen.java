package org.example.elements.hit;

import org.example.CompositeShape;
import org.example.Main;
import org.example.Round;
import org.example.ShapeBuilder;

public class HitsKen extends CompositeShape {   //剑玉剑气
    int side;
    int frame;
    int user;
    float rot;

    public HitsKen(float X, float Y, float R, int S, int USER) {
        super(X, Y);
        frame = 0;
        rot = (R % 360 + 360) % 360;
        user = USER;
        side = S;
        ShapeBuilder.into(this).sector(0, 0, 50, 90, rot > 90+side && rot < 270-side ? rot + 90 : rot - 90);
        id = Main.addElement(this);
        Main.atk[side].addShape(this);
    }

    public void kill() {
        Main.elements.remove(id);
        Main.atk[side].removeShape(this);
    }

    @Override
    public void step(){
        if (Main.elements.containsKey(user)){
            this.x = Main.elements.get(user).x;
            this.y = Main.elements.get(user).y;
        }
        int wrk = rot >= 90 && rot <= 270 ? 1 : -1;
        switch (frame){
            case 0:{
                ShapeBuilder.into(this).sector(0, 0, 58, 20, rot + wrk * 35)
                        .sector(0, 0, 61, 25, rot + wrk * 12.5F);
                break;
            }
            case 1:{
                ShapeBuilder.into(this).sector(0, 0, 64, 30, rot - wrk * 15);
                break;
            }
            case 2:{
                removeShape(getShapes().getFirst());
                break;
            }
            case 3:{
                removeShape(getShapes().getFirst());
                removeShape(getShapes().getFirst());
                break;
            }
            case 4:{
                removeShape(getShapes().getFirst());
                kill();
                break;
            }
        }
        frame++;
    }
}
