package org.example.elements.units;

import org.example.Game;
import org.example.elements.Ball;
import org.example.elements.atk.CannonBullet;

public class CannonBall extends Ball {  // 炮玉
    public CannonBall(Game game, float X, float Y, int R, int S, int TYPE) {
        super(game, X, Y, R, S, TYPE);
        speed = 120;
    }

    @Override
    public void stepEx() {      //使用诱导公式简化了计算
        if (this.cnt == this.speed) {
            this.cnt = 0;
            int flipped = this.rot >= 90 + this.side && this.rot <= 270 + this.side ? -1 : 1;
            float spawnX = this.x + sin_rot * 38 * flipped;
            float spawnY = this.y - cos_rot * 38 * flipped;
            new CannonBullet(this.game, spawnX, spawnY, this.side).setVecMult(cos_rot, sin_rot, 10);
        }
    }
}
