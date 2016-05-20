package net.uncontended.precipice.metrics.histogram;

public interface WritableLatency<T> {

    public void write(T result, long number, long nanoLatency, long nanoTime);
}
