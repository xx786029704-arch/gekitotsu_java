package org.example.elements.wall;

import org.example.Game;
import org.example.elements.Wall;

public class Turbo extends Wall {
    public Turbo(Game game, float X, float Y, int S, int TYPE) {
        super(game, X, Y, S, TYPE);
        max_hp = 150;
        hp = 150;
        this.game.bases[S].axl+=2;
    }

    public void kill() {
        super.kill();
        this.game.bases[side].axl-=2;
    }

    @Override
    public void step(){
        if (this.game.bases[side] == null){
            super.kill();
            return;
        }
        this.move(this.game.bases[side].base_move_x,this.game.bases[side].base_move_y);
        stepEx();
        if (this.game.atk[1-side].hitTestPoint(this.x - 6, this.y - 6) ||
                this.game.atk[1-side].hitTestPoint(this.x + 6, this.y - 6) ||
                this.game.atk[1-side].hitTestPoint(this.x - 6, this.y + 6) ||
                this.game.atk[1-side].hitTestPoint(this.x + 6, this.y + 6)) {
            hp--;
            if (this.hp <= 0) {
                kill();
            }
        }
        if (hp < max_hp && this.game.repair[side].hitTestPoint(this.x, this.y)) {
            hp ++;
        }
    }
}
