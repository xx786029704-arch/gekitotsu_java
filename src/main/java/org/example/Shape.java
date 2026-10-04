package org.example;

import java.awt.*;

public abstract class Shape {   //最基本的抽象形状类
    public float x;
    public float y;
    public int id;

    public Shape(float X, float Y) {
        x = X;
        y = Y;
    }

    /*
        可能的优化：可以使用n叉树分而治之图形碰撞，太大的（如核弹）或处在边界的留在父节点，检测时只检测当前节点和父节点所有图形
        可能的优化：使用对象池处理弹幕产生和销毁
     */

    public abstract boolean hitTestPoint(float X, float Y);     //点碰撞

    public void move(float X, float Y) {
        x += X;
        y += Y;
    }

    public void moveTo(float X, float Y) {
        x = X;
        y = Y;
    }

    public void xySync(){       //复刻 flash引擎量子化特性，在任何非0.05倍数移动时调用
        x = (int) (20 * x) * 0.05F;
        y = (int) (20 * y) * 0.05F;
    }

    public void xSync(){       //仅针对 x 值的量子化
        x = (int) (20 * x) * 0.05F;
    }

    public void ySync(){       //仅针对 y 值的量子化
        y = (int) (20 * y) * 0.05F;
    }

    public void step(){     //每帧执行的代码
    }
}
