package org.example.elements.units;

import org.example.GameTask;
import org.example.elements.Ball;

public class NieBall extends Ball {     //贽玉
    public int alarm = -1;

    public NieBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        hp = 30;
        max_hp = 30;
    }

    @Override
    public void stepEx(){
        if (alarm > 0) {
            alarm--;
            if (alarm == 0){
                hp = 0;
            }
        }
    }
}
