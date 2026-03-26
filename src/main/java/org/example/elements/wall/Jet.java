package org.example.elements.wall;

import org.example.Game;
import org.example.elements.Wall;

public class Jet extends Wall {
    public Jet(Game game, float X, float Y, int S, int TYPE) {
        super(game, X, Y, S, TYPE);
        max_hp = 15;
        hp = 15;
        this.game.bases[S].axl+=1;
    }

    public void kill() {
        super.kill();
        this.game.bases[side].axl-=1;
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
                return;
            }
        }
        if (hp < max_hp && this.game.repair[side].hitTestPoint(this.x, this.y)) {
            hp ++;
        }
    }
}
