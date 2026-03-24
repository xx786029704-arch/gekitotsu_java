package org.example.elements.wall;

import org.example.Main;
import org.example.elements.Wall;

public class Jet extends Wall {
    public Jet(float X, float Y, int S, int TYPE) {
        super(X, Y, S, TYPE);
        max_hp = 15;
        hp = 15;
        Main.bases[S].axl+=1;
    }

    public void kill() {
        super.kill();
        Main.bases[side].axl-=1;
    }

    @Override
    public void step(){
        if (Main.bases[side] == null){
            super.kill();
            return;
        }
        this.move(Main.bases[side].base_move_x,Main.bases[side].base_move_y);
        stepEx();
        if (Main.atk[1-side].hitTestPoint(this.x - 6, this.y - 6) ||
                Main.atk[1-side].hitTestPoint(this.x + 6, this.y - 6) ||
                Main.atk[1-side].hitTestPoint(this.x - 6, this.y + 6) ||
                Main.atk[1-side].hitTestPoint(this.x + 6, this.y + 6)) {
            hp--;
            if (this.hp <= 0) {
                kill();
                return;
            }
        }
        if (hp < max_hp && Main.repair[side].hitTestPoint(this.x, this.y)) {
            hp ++;
        }
    }
}
