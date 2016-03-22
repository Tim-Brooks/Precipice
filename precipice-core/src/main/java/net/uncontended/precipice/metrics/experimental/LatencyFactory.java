package net.uncontended.precipice.metrics.experimental;

import net.uncontended.precipice.metrics.LatencyMetrics;

@FunctionalInterface
public interface LatencyFactory {

    <T extends Enum<T>> LatencyMetrics<T> newLatency(Class<T> clazz, long nanoTime);
}
