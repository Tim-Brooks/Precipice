package net.uncontended.precipice.metrics.latency;

public interface WritableLatency<T> {

    public void write(T result, long number, long nanoLatency, long nanoTime);
}
