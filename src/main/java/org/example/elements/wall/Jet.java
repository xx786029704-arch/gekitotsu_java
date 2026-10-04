package org.example.elements.wall;

import org.example.GameTask;
import org.example.elements.Wall;

public class Jet extends Wall {
    public Jet(GameTask GAME, float X, float Y, int S, int TYPE) {
        super(GAME, X, Y, S, TYPE);
        max_hp = 15;
        hp = 15;
        game.bases[S].axl+=1;
    }

    public void kill() {
        super.kill();
        game.bases[side].axl-=1;
    }

    @Override
    public void step(){
        if (game.bases[side] == null){
            super.kill();
            return;
        }
        this.move(game.bases[side].base_move_x,game.bases[side].base_move_y);
        stepEx();
        if (game.atk[1-side].hitTestPoint(this.x - 6, this.y - 6) ||
                game.atk[1-side].hitTestPoint(this.x + 6, this.y - 6) ||
                game.atk[1-side].hitTestPoint(this.x - 6, this.y + 6) ||
                game.atk[1-side].hitTestPoint(this.x + 6, this.y + 6)) {
            hp--;
            if (this.hp <= 0) {
                kill();
                return;
            }
        }
        if (hp < max_hp && game.repair[side].hitTestPoint(this.x, this.y)) {
            hp ++;
        }
    }
}
