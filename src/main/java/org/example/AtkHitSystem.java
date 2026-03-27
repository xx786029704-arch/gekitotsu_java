package org.example;

import org.example.elements.Bullet;
import org.example.elements.hit.HitsNagi;

public class AtkHitSystem extends HitSystem{
    public AtkHitSystem(float X, float Y) {
        super(X, Y);
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
                    if (s.x < mid + 84){
                        upper_left.add(s);
                    }
                    if (s.x > mid - 84){
                        upper_right.add(s);
                    }
                }
                if (s.y > -84){
                    if (s.x < mid + 84){
                        left.add(s);
                    }
                    if (s.x > mid - 84){
                        right.add(s);
                    }
                }
                continue;
            }
            common.add(s);
        }
    }
}
