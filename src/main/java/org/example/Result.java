package org.example;

public class Result {
    public final int status;//-2=异常 -1=超时 0=平局 1=1P胜利 2=2P胜利
    public final int winnerHp;
    public final int framePassed;
    public final float timeUsed;

    public Result(int status, int winnerHp, int framePassed, float timeUsed) {
        this.status = status;
        this.winnerHp = winnerHp;
        this.framePassed = framePassed;
        this.timeUsed = timeUsed;
    }

    public String getSimpleResult(){
        if (Main.SHOW_REMAIN_HP) return status == 1 ? (winnerHp + ",") : (status == 2 ? (-winnerHp + ",") : (status == 0 ? "0," : "?,"));
        return status == 1 ? "1," : (status == 2 ? "2," : (status == 0 ? "d," : "?,"));
    }
}