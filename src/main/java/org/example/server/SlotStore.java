package org.example.server;

import org.example.CompiledFort;

import java.util.List;

public class SlotStore {
    private volatile List<CompiledFort> p1 = List.of();
    private volatile List<String> p1Names = List.of();
    private volatile List<CompiledFort> p2 = List.of();
    private volatile List<String> p2Names = List.of();

    public void setP1(List<CompiledFort> forts, List<String> names) {
        this.p1 = List.copyOf(forts);
        this.p1Names = List.copyOf(names);
    }

    public List<CompiledFort> getP1() { return p1; }
    public List<String> getP1Names() { return p1Names; }
    public void clearP1() {
        p1 = List.of();
        p1Names = List.of();
    }

    public void setP2(List<CompiledFort> forts, List<String> names) {
        this.p2 = List.copyOf(forts);
        this.p2Names = List.copyOf(names);
    }

    public List<CompiledFort> getP2() { return p2; }
    public List<String> getP2Names() { return p2Names; }
    public void clearP2() {
        p2 = List.of();
        p2Names = List.of();
    }
}
