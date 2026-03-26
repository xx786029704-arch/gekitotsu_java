package org.example.elements.units;

import org.example.Game;
import org.example.elements.Ball;
import org.example.elements.atk.GekiBullet;

public class GekiBall extends Ball {   //击玉
    public GekiBall(Game game, float X, float Y, int R, int S, int TYPE) {   //初始化
        super(game, X, Y, R, S, TYPE);
        speed = 120;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 45;
            float spawnY = this.y + sin_rot * 45;
            new GekiBullet(this.game, spawnX, spawnY, this.side, this.cos_rot, this.sin_rot);
        }
    }
}
