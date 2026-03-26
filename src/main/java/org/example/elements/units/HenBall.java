package org.example.elements.units;

import org.example.Game;
import org.example.elements.Ball;
import org.example.Utils;
import org.example.elements.atk.WeakCreature;
import org.example.elements.atk.SolidCreature;
import org.example.elements.atk.ShockCreature;
import org.example.elements.atk.DrillCreature;

public class HenBall extends Ball {   //变玉
    private Utils seeder;
    public HenBall(Game game, float X, float Y, int R, int S, int TYPE) {   //初始化
        super(game, X, Y, R, S, TYPE);
        seeder = new Utils();
        speed = 100;
    }

    public void setSeed(int seed) {
        seeder.setSeed(seed);
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed + 8) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 25.F;
            float spawnY = this.y + sin_rot * 25.F;
            int spawnType = Math.round(Utils.random(this.seeder) * 7);
            if (spawnType == 2 || spawnType == 3) new DrillCreature(this.game, spawnX, spawnY, this.side, this.rot);
            else if (spawnType == 4) new SolidCreature(this.game, spawnX, spawnY, this.side, this.rot);
            else if (spawnType == 5) new ShockCreature(this.game, spawnX, spawnY, this.side, this.rot);
            else new WeakCreature(this.game, spawnX, spawnY, this.side, this.rot);
        }
    }
}