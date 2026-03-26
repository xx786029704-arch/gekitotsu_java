package org.example.elements.wall;

import org.example.elements.Wall;
import org.example.Game;

public class Paper extends Wall {   //纸壁
    public Paper(Game game, float X, float Y, int S, int TYPE) {
        super(game, X, Y, S, TYPE);
        max_hp = 1;
        hp = 1;
    }

    //可能的优化：重写step，不再检测血量而是受伤即破
}
