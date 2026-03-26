package org.example;

import org.example.Settings;
import org.example.Game;
import java.util.Scanner;
import org.example.Result;

public class Main {

    public static void main(String[] args) {
        String default_code1 = "000vrYt00j5gp008kwp00F8gr00H0hj1iRlr41ScWra2o3HValFmBjalaz599liN669liTNV9leAY99laiWx9l5YQm9kRiAx9kRp2Y9kN6dc9kNcV29kIUaO7k9z2e7kdYzQ7kip8t7ke0UO7k9BIa7ke2gM7kirOp7ke3BK7k9Ep67ke4WI7kiuvl7ke6z5";
        String default_code2 = "000jmlj1iBdPp00tiAang83uang5mya2aF0ka2SGiz40vhTJt008lb1nfMDo1nfJpK7kdMu97kdOTG7kdPHR7kdS8p7kicuY7kieUw7kifIH7kii9f9kMTVE9kIxRh9kIDf29kIIBT9l9VVe9l5zAu9l5EXf9l5Kl79plrM79plH6pp00tl0r00gpVa9JXdDr01ASS";
        String[] input;
        Scanner scanner = new Scanner(System.in);
        if (default_code1.isEmpty() || default_code2.isEmpty()) {   //处理代码导入
            System.out.println("请输入对战代码：(输入格式: 1P代码 vs 2P代码)");
            input = scanner.nextLine().split(" vs ");
        } else {
            input = new String[]{default_code1, default_code2};
        }
        Result r = (new Game()).run(input[0], input[1]);
        switch (r.status) {
            case -2: {
                System.out.println("结果:未结束");
                break;
            }
            case -1: {
                System.out.println("结果:对局未能进行");
                break;
            }
            case 0: {
                System.out.println("结果:平局");
                break;
            }
            case 1: {
                System.out.println("结果:1P胜利");
                break;
            }
            case 2: {
                System.out.println("结果:2P胜利");
                break;
            }
        }
        System.out.println("胜者血量:" + r.winnerHp);
        System.out.println("帧数:" + r.framePassed);
        System.out.println("所用时间:" + r.timeUsed);
    }
}
