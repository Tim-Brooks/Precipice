package net.uncontended.precipice.metrics;

import net.uncontended.precipice.metrics.LatencyMetrics;

@FunctionalInterface
public interface LatencyFactory {

    <T extends Enum<T>> LatencyMetrics<T> newLatency(Class<T> clazz, long nanoTime);
}
