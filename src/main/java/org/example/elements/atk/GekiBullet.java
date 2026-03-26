package org.example.elements.atk;

import org.example.Game;
import org.example.Round;
import org.example.Utils;
import org.example.elements.Bullet;
import org.example.elements.hit.HitsBomb;

public class GekiBullet extends Bullet {   //击玉火箭弹
    private float speed;
    public float cos_rot;
    public float sin_rot;

    public GekiBullet(Game game, float X, float Y, int S, float cos_rot, float sin_rot) {   //初始化
        super(game, X, Y, S);
        this.cos_rot = cos_rot;
        this.sin_rot = sin_rot;
        this.speed = 5;
        this.xs = cos_rot * 5;
        this.ys = sin_rot * 5;
    }

    @Override
    public void step() {   //每帧逻辑
        if (this.y < -1200 || this.x > 2560 || this.x < -640) {
            kill();
            return;
        }
        if (this.game.team[1 - this.side].hitTestPoint(this.x, this.y) || this.y > 570 || this.gei_flg == 2) {
            new HitsBomb(this.game, this.x, this.y, this.side);
            kill();
            return;
        }
        if (this.speed < 25F) {
            this.speed = this.speed + 1F;
        }
        this.xs = cos_rot * this.speed;
        this.ys = sin_rot * this.speed;
        move();
    }

    @Override
    public void reflect(int from_rot){
        betray();
        cos_rot = Utils.cos(from_rot);
        sin_rot = Utils.sin(from_rot);
        this.xs = cos_rot * this.speed;
        this.ys = sin_rot * this.speed;
    }
}
