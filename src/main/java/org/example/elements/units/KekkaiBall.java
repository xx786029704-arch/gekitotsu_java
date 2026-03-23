package org.example.elements.units;

import org.example.Main;
import org.example.elements.Ball;

public class KekkaiBall extends Ball {   //界玉
    public int hurt_time = 0;

    public KekkaiBall(float X, float Y, float R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        speed = 0;
        hp = 20;
        max_hp = 20;
        Main.kekkaiIds[this.side].add(this.id);
    }

    @Override
    public void kill() {
        Main.kekkaiIds[this.side].remove((Integer) this.id);
        super.kill();
    }

    @Override
    public void hurt(boolean is_crash){
        hurt_time = 4;
    }

    @Override
    public void stepEx(){
        if(hurt_time > 0){
            hurt_time--;
        }
    }
}
