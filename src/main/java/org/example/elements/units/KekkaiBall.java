package org.example.elements.units;

import org.example.Game;
import org.example.elements.Ball;

public class KekkaiBall extends Ball {   //界玉
    public int hurt_time = 0;

    public KekkaiBall(Game game, float X, float Y, int R, int S, int TYPE) {   //初始化
        super(game, X, Y, R, S, TYPE);
        speed = 0;
        hp = 20;
        max_hp = 20;
        this.game.kekkaiIds[this.side].add(this.id);
    }

    @Override
    public void kill() {
        this.game.kekkaiIds[this.side].remove((Integer) this.id);
        super.kill();
    }

    @Override
    public void hurt(boolean is_crash){
        hurt_time = 5;
    }

    @Override
    public void stepEx(){
        if(hurt_time > 0){
            hurt_time--;
        }
    }
}
