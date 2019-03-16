package ru.byprogminer.Lab6_Programming;

public class PriorityThing<P extends Number, T> implements Comparable<PriorityThing<P, T>> {

    private final P priority;
    private final T thing;

    public PriorityThing(P priority, T thing) {
        this.priority = priority;
        this.thing = thing;
    }

    @Override
    public int compareTo(PriorityThing<P, T> that) {
        return (int) (priority.longValue() - that.priority.longValue());
    }

    public P getPriority() {
        return priority;
    }

    public T getThing() {
        return thing;
    }
}
