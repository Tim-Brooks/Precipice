package net.uncontended.precipice.metrics;

@FunctionalInterface
public interface LatencyFactory {

    <T extends Enum<T>> WritableLatencyMetrics<T> newLatency(Class<T> clazz);
}
