package net.uncontended.precipice.metrics.counts;

import net.uncontended.precipice.metrics.tools.IntervalIterator;
import net.uncontended.precipice.metrics.tools.Rolling;

public class RollingCounts<T extends Enum<T>> implements WritableCounts<T>, Rolling<PartitionedCount<T>> {
    @Override
    public void write(T result, long number, long nanoTime) {

    }

    @Override
    public IntervalIterator<PartitionedCount<T>> intervals() {
        return null;
    }

    @Override
    public IntervalIterator<PartitionedCount<T>> intervals(long nanoTime) {
        return null;
    }

    @Override
    public PartitionedCount<T> current() {
        return null;
    }

    @Override
    public PartitionedCount<T> current(long nanoTime) {
        return null;
    }

    @Override
    public PartitionedCount<T> total() {
        return null;
    }
}
