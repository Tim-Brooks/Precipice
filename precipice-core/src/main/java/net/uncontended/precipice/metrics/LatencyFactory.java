package net.uncontended.precipice.metrics;

@FunctionalInterface
public interface LatencyFactory {

    <T extends Enum<T>> LatencyMetrics<T> newLatency(Class<T> clazz);
}
