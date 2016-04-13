package net.uncontended.precipice.metrics;

@FunctionalInterface
public interface LatencyFactory {

    <T extends Enum<T>> PartitionedHistogram<T> newLatency(Class<T> clazz);
}
