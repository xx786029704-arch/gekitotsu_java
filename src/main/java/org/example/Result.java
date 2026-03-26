package org.example;

public class Result {
    public final int status;//-2=超时 -1=异常 0=平局 1=1P胜利 2=2P胜利
    public final int winnerHp;
    public final int framePassed;
    public final float timeUsed;
    public Result(int status,int winnerHp,int framePassed,float timeUsed) {
        this.status = status;
        this.winnerHp = winnerHp;
        this.framePassed = framePassed;
        this.timeUsed = timeUsed;
    }
}
