package org.example;

import org.example.elements.Bullet;

public class AtkHitSystem extends HitSystem{
    public AtkHitSystem(float X, float Y) {
        super(X, Y);
    }

    @Override
    public void resign(){
        common.clear();
        upper.clear();
        left.clear();
        right.clear();
        for (Shape s : shapes){
            if (s instanceof Bullet bullet){
                if (bullet.y < -60){
                    upper.add(bullet);
                    continue;
                } else if (bullet.y > 60){
                    if (bullet.x < mid - 60){
                        left.add(bullet);
                        continue;
                    } else if (bullet.x > mid + 60){
                        right.add(bullet);
                        continue;
                    }
                }
            }
            common.add(s);
        }
    }
}
