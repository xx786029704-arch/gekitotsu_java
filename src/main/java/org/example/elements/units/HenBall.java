package org.example.elements.units;

import org.example.GameTask;
import org.example.elements.Ball;
import org.example.Utils;
import org.example.elements.atk.WeakCreature;
import org.example.elements.atk.SolidCreature;
import org.example.elements.atk.ShockCreature;
import org.example.elements.atk.DrillCreature;

public class HenBall extends Ball {   //变玉
    public HenBall(GameTask GAME, float X, float Y, int R, int S, int TYPE) {
        super(GAME, X, Y, R, S, TYPE);
        speed = 100;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed + 8) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 25.F;
            float spawnY = this.y + sin_rot * 25.F;
            int spawnType=Math.round(game.seeder.random()*7);
            if (spawnType==2||spawnType==3) new DrillCreature(game, spawnX,spawnY,this.side,this.rot);
            else if (spawnType==4) new SolidCreature(game, spawnX,spawnY,this.side,this.rot);
            else if (spawnType==5) new ShockCreature(game, spawnX,spawnY,this.side,this.rot);
            else new WeakCreature(game, spawnX,spawnY,this.side,this.rot);
        }
    }
}