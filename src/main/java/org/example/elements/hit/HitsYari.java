package org.example.elements.hit;

import org.example.CompositeShape;
import org.example.Game;
import org.example.ShapeBuilder;
import org.example.YariBase;

public class HitsYari extends CompositeShape {   //剑玉剑气
    private final int side;
    int frame;
    private final int user;
    private final int rot;
    protected final Game game;

    public HitsYari(Game game, float X, float Y, int R, int S, int USER) {
        super(X, Y);
        this.game = game;
        xySync();
        frame = 0;
        rot = (R % 360 + 360) % 360;
        user = USER;
        side = S;
        ShapeBuilder.into(this).shape(new YariBase(X, Y, R, .5f, -29.5f, 3.4f));
        id = this.game.addElement(this);
        this.game.atk[side].addShape(this);
    }

    public void kill() {
        this.game.elements.remove(id);
        this.game.atk[side].removeShape(this);
    }

    @Override
    public void step(){
        if (this.game.elements.containsKey(user)) {
            moveTo(this.game.elements.get(user).x, this.game.elements.get(user).y);
        }

        switch (frame){
            case 0:{
                ShapeBuilder.into(this).shape(new YariBase(this.x,this.y,this.rot,.625f,-33.f,3.4f))
                        .shape(new YariBase(this.x,this.y,this.rot,.75f,-32.6f,3.5f));
                break;
            }
            case 1:{
                removeShape(getShapes().getFirst());
                removeShape(getShapes().getFirst());
                removeShape(getShapes().getFirst());
                ShapeBuilder.into(this).shape(new YariBase(this.x,this.y,this.rot,.75f,-26.6f,3.5f))
                        .shape(new YariBase(this.x,this.y,this.rot,.875f,-26.55f,3.6f))
                        .shape(new YariBase(this.x,this.y,this.rot,1.f,-22.f,3.5f));
                break;
            }
            case 2:{
                removeShape(getShapes().getFirst());
                removeShape(getShapes().getFirst());
                removeShape(getShapes().getFirst());
                ShapeBuilder.into(this).shape(new YariBase(this.x,this.y,this.rot,1.f,-21.8f,3.5f))
                        .shape(new YariBase(this.x,this.y,this.rot,1.f,0.f,3.6f));
                break;
            }
            case 3:{
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