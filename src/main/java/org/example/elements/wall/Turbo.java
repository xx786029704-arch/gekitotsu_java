package org.example.elements.wall;

import org.example.Main;
import org.example.elements.Wall;

public class Turbo extends Wall {
    public Turbo(float X, float Y, int S, int TYPE) {
        super(X, Y, S, TYPE);
        max_hp = 150;
        hp = 150;
        Main.bases[S].axl+=2;
    }

    public void kill() {
        super.kill();
        Main.bases[side].axl-=2;
    }

    @Override
    public void step(){
        this.move(Main.bases[side].base_move_x,Main.bases[side].base_move_y);
        stepEx();
        if (Main.atk[1-side].hitTestPoint(this.x - 6, this.y - 6) ||
                Main.atk[1-side].hitTestPoint(this.x + 6, this.y - 6) ||
                Main.atk[1-side].hitTestPoint(this.x - 6, this.y + 6) ||
                Main.atk[1-side].hitTestPoint(this.x + 6, this.y + 6)) {
            hp--;
            if (this.hp <= 0) {
                kill();
            }
        }
        if (hp < max_hp && Main.repair[side].hitTestPoint(this.x, this.y)) {
            hp ++;
        }
    }
}
