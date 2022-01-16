package dev.laarryy.atropos.utils;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public class Pair<T1, T2> {
    private final T1 t1;
    private final T2 t2;

    private final int hc;

    public Pair(T1 t1, T2 t2) {
        this.t1 = t1;
        this.t2 = t2;

        this.hc = Objects.hash(t1, t2);
    }

    public @Nullable T1 getT1() { return t1; }

    public @Nullable T2 getT2() { return t2; }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(t1, pair.t1) &&
                Objects.equals(t2, pair.t2);
    }

    public int hashCode() { return hc; }

    public String toString() {
        return "Pair{" +
                "t1=" + t1 +
                ", t2=" + t2 +
                '}';
    }
}

