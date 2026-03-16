package org.example;

import org.example.elements.base;
import org.example.elements.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Main {
    public static final boolean ENABLE_VISUALIZATION = true;
    public static final int LOGIC_TPS = 60;

    static boolean norikomi_flg = false;
    static int j;
    static float wrk;
    public static int[] hp = {100, 100};
    public static int[] hp0_flg = {0, 0};
    public static boolean[] dokkan_flg = {false, false};
    public static base[] bases;
    public static core[] cores;
    public static int time = 0;
    public static List<Shape> elements = new CopyOnWriteArrayList<>();
    public static CompositeShape[] wall = {new CompositeShape(0,0), new CompositeShape(0,0)};
    public static CompositeShape[] unit = {new CompositeShape(0,0), new CompositeShape(0,0)};
    public static CompositeShape[] shield = {new CompositeShape(0,0), new CompositeShape(0,0)};
    public static CompositeShape[] atk = {new CompositeShape(0,0), new CompositeShape(0,0)};
    public static CompositeShape[] team = {new CompositeShape(0,0), new CompositeShape(0,0)};

    public static void main(String[] args) {
        for (int i = 0; i < 2; i++){
            team[i].addShape(wall[i]);
            team[i].addShape(unit[i]);
            team[i].addShape(shield[i]);
            team[i].addShape(atk[i]);
        }

        bases = new base[]{new base(240, 532, 0), new base(1680, 532, 1)};
        cores = new core[]{new core(240, 332, 0), new core(1680, 332, 1)};
        if (ENABLE_VISUALIZATION) {
            java.awt.EventQueue.invokeLater(() -> {
                GameWindow window = new GameWindow(1920, 1080, 60);
                window.setVisible(true);
            });
        }
        long timePerTick = LOGIC_TPS > 0 ? 1000000000L / LOGIC_TPS : 0;
        long lastTime = System.nanoTime();

        while (time < 65536){
            long now = System.nanoTime();
            if (timePerTick == 0 || now - lastTime >= timePerTick) {
                lastTime = now;
                time++;

                if (hp0_flg[0] == 0 && hp0_flg[1] == 0 && bases[1].x - bases[1].xs - (bases[0].x + bases[0].xs) <= 380) {
                    norikomi_flg = true;
                    System.out.println(time);
                    j = 0;
                    while (j <= 1) {
                        wrk = (float) (Math.floor(bases[(1 - j)].xs * 5) + 1);
                        wrk = Math.round(wrk);
                        if (wrk < 0) {
                            wrk = 0;
                        }
                        hp[j] -= (int) wrk;
                        dokkan_flg[j] = true;
                        j++;
                    }
                    wrk = bases[0].xs;
                    bases[0].xs = -bases[1].xs;
                    if (bases[0].xs > -1) { bases[0].xs = -1; }
                    bases[1].xs = -wrk;
                    if (bases[1].xs > -1) { bases[1].xs = -1; }
                    bases[0].ys = (-bases[0].xs) * 2;
                    bases[1].ys = (-bases[1].xs) * 2;
                }

                for (Shape s : elements){
                    s.step();
                }
            } else {
                Thread.yield();
            }
        }
    }
}