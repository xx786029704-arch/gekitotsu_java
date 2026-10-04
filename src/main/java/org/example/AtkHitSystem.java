package org.example;

import org.example.elements.Bullet;
import org.example.elements.hit.HitsNagi;

public class AtkHitSystem extends HitSystem{
    public AtkHitSystem(GameTask GAME, float X, float Y) {
        super(GAME, X, Y);
    }

    @Override
    public void resign(){
        common.clear();
        upper_left.clear();
        upper_right.clear();
        left.clear();
        right.clear();
        for (Shape s : shapes){
            if (s instanceof Bullet || s instanceof HitsNagi){
                if (s.y < 84){
                    if (s.x < game.mid + 84){
                        upper_left.add(s);
                    }
                    if (s.x > game.mid - 84){
                        upper_right.add(s);
                    }
                }
                if (s.y > -84){
                    if (s.x < game.mid + 84){
                        left.add(s);
                    }
                    if (s.x > game.mid - 84){
                        right.add(s);
                    }
                }
                continue;
            }
            common.add(s);
        }
    }
}
