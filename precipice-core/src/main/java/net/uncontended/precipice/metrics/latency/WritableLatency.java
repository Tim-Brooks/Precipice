package net.uncontended.precipice.metrics.latency;

import net.uncontended.precipice.metrics.Metrics;

public interface WritableLatency<T extends Enum<T>> extends Metrics<T> {

    public void write(T result, long number, long nanoLatency, long nanoTime);
}
