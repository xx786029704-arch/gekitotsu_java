package org.example.elements.units;

import org.example.elements.Ball;
import org.example.elements.atk.GekiBullet;
import org.example.elements.atk.SearchBullet;

public class SearchBall extends Ball {   //查玉
    public SearchBall(float X, float Y, int R, int S, int TYPE) {   //初始化
        super(X, Y, R, S, TYPE);
        speed = 100;
    }

    @Override
    public void stepEx() {   //攻击逻辑
        if (this.cnt == this.speed + 4) {
            this.cnt = 0;
            float spawnX = this.x + cos_rot * 35;
            float spawnY = this.y + sin_rot * 35;
            new SearchBullet(spawnX, spawnY, this.side, this.cos_rot, this.sin_rot);
        }
    }
}
