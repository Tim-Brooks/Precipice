package net.uncontended.precipice.metrics.counts;

import net.uncontended.precipice.metrics.AbstractMetrics;
import net.uncontended.precipice.metrics.tools.Allocator;
import net.uncontended.precipice.metrics.tools.IntervalIterator;
import net.uncontended.precipice.metrics.tools.Rolling;
import net.uncontended.precipice.metrics.tools.RollingMetrics;

public class RollingCounts<T extends Enum<T>> extends AbstractMetrics<T> implements WritableCounts<T>, Rolling<PartitionedCount<T>> {

    private final RollingMetrics<PartitionedCount<T>> rolling;

    public RollingCounts(Class<T> clazz) {
        this(clazz, Counters.longAdder(clazz));
    }

    public RollingCounts(Class<T> clazz, Allocator<PartitionedCount<T>> allocator) {
        super(clazz);
        rolling = new RollingMetrics<PartitionedCount<T>>(allocator);
    }

    @Override
    public void write(T metric, long number, long nanoTime) {
        rolling.current(nanoTime).add(metric, number);
    }

    @Override
    public PartitionedCount<T> current() {
        return rolling.current();
    }

    @Override
    public PartitionedCount<T> current(long nanoTime) {
        return rolling.current(nanoTime);
    }

    @Override
    public IntervalIterator<PartitionedCount<T>> intervals() {
        return rolling.intervals();
    }

    @Override
    public IntervalIterator<PartitionedCount<T>> intervals(long nanoTime) {
        return rolling.intervals(nanoTime);
    }
}
