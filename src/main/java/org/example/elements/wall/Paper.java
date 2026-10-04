package org.example.elements.wall;

import org.example.GameTask;
import org.example.elements.Wall;

public class Paper extends Wall {   //纸壁
    public Paper(GameTask GAME, float X, float Y, int S, int TYPE) {
        super(GAME, X, Y, S, TYPE);
        max_hp = 1;
        hp = 1;
    }

    //可能的优化：重写step，不再检测血量而是受伤即破
}
