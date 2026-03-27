package org.example.elements.hit;

import org.example.GameTask;
import org.example.Main;
import org.example.Sector;
import org.example.Shape;

import java.awt.*;

public class HitsNagi extends Sector {
    private final int side;
    private int frame;
    private final int user;
    private final boolean flipped;
    private final float cos_rot;
    private final float sin_rot;
    private final GameTask game;

    public HitsNagi(GameTask GAME, float X, float Y, int R, int S, int USER, float _cos_rot, float _sin_rot) {
        super(X, Y, 63, 45, 292.5F);
        xySync();
        game = GAME;
        frame = 0;
        user = USER;
        side = S;
        cos_rot = _cos_rot;
        sin_rot = _sin_rot;
        flipped = R >= 90 + side && R <= 270 + side;
        id = game.addElement(this);
        game.atk[side].addShape(this);
    }

    public void kill() {
        game.elements.remove(id);
        game.atk[side].removeShape(this);
    }

    @Override
    public void step(){
        Shape shape = game.elements.get(user);
        if (shape != null){
            this.x = shape.x;
            this.y = shape.y;
        }
        if (frame++ == 9){
            kill();
        }
    }

    @Override
    public boolean hitTestPoint(float X, float Y){
        if (X - x > 81.4F || Y - y > 81.4F || X - x < -81.4F || Y - y < -81.4F){
            return false;
        }
        float dx = ((X - x) * cos_rot + (Y - y) * sin_rot);
        float dy = ((Y - y) * cos_rot - (X - x) * sin_rot);
        if (!flipped){
            dy = -dy;
        }
        dx -= 18.4F;
        switch (frame){
            case 0:{
                if (dx * dy < dx * dx) return false;
                break;
            }
            case 1:{
                if (dx * dy < 0F) return false;
                break;
            }
            case 2, 6:{
                if (dx * dy < -dx * dx) return false;
                break;
            }
            case 3, 7:{
                if (dx * dy > dx * dx) return false;
                break;
            }
            case 4:{
                if (dx * dy > dy * dy) return false;
                break;
            }
            case 5:{
                if (dx * dy < -dy * dy) return false;
                break;
            }
            case 8:{
                if (dx * dy > 0F) return false;
                break;
            }
            case 9:{
                if (dx * dy > -dx * dx) return false;
                break;
            }

        }
        return dx * dx + dy * dy <= 3969F;
    }
}
