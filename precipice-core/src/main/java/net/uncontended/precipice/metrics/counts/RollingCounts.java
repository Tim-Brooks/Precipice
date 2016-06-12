package net.uncontended.precipice.metrics.counts;

import net.uncontended.precipice.metrics.AbstractMetrics;
import net.uncontended.precipice.metrics.tools.IntervalIterator;
import net.uncontended.precipice.metrics.tools.Rolling;

public class RollingCounts<T extends Enum<T>> extends AbstractMetrics<T> implements WritableCounts<T>, Rolling<PartitionedCount<T>> {

    public RollingCounts(Class<T> clazz) {
        super(clazz);
    }

    @Override
    public void write(T result, long number, long nanoTime) {

    }

    @Override
    public PartitionedCount<T> current() {
        return null;
    }

    @Override
    public IntervalIterator<PartitionedCount<T>> intervals() {
        return null;
    }

    @Override
    public IntervalIterator<PartitionedCount<T>> intervals(long nanoTime) {
        return null;
    }
}
