package net.uncontended.precipice.metrics.counts;

import net.uncontended.precipice.metrics.Metrics;

public interface WritableCounts<T extends Enum<T>> extends Metrics<T> {

    public void write(T result, long number, long nanoTime);

}
