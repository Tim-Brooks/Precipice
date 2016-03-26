package net.uncontended.precipice.metrics;

import java.util.Iterator;

public interface IntervalIterable<T> extends Iterable<T>, Iterator<T> {
    @Override
    boolean hasNext();

    @Override
    T next();

    @Override
    void remove();

    @Override
    Iterator<T> iterator();

    long intervalStart();

    long intervalEnd();
}
