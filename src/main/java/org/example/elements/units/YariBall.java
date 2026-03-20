package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.hit.HitsYari;

public class YariBall extends Ball {     //枪玉
    //TODO：枪玉的对局表现目前和实际游戏对局出入很大，检查判定范围设计的没问题，那么问题出在哪？
    public YariBall(float X, float Y, float R, int S, int TYPE) {
        super(X, Y, R, S, TYPE);
        speed = 15;
    }

    @Override
    public void stepEx(){
        if (this.cnt == this.speed)
        {
            this.cnt = 0;
            new HitsYari(x, y, rot, side, id);
        }
    }
}