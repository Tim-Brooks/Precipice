package net.uncontended.precipice.metrics.histogram;

import net.uncontended.precipice.metrics.AbstractMetrics;
import net.uncontended.precipice.metrics.tools.Allocator;
import net.uncontended.precipice.metrics.tools.IntervalIterator;
import net.uncontended.precipice.metrics.tools.Rolling;
import net.uncontended.precipice.metrics.tools.RollingMetrics;

public class RollingHistogram<T extends Enum<T>> extends AbstractMetrics<T> implements WritableLatency<T>, Rolling<PartitionedHistogram<T>> {

    private final RollingMetrics<PartitionedHistogram<T>> rolling;

    public RollingHistogram(Class<T> clazz) {
        this(clazz, Latency.atomicHDRHistogram(clazz));
    }

    public RollingHistogram(Class<T> clazz, Allocator<PartitionedHistogram<T>> allocator) {
        super(clazz);
        rolling = new RollingMetrics<PartitionedHistogram<T>>(allocator);
    }

    @Override
    public void write(T result, long number, long nanoLatency, long nanoTime) {
        rolling.current(nanoTime).record(result, number, nanoLatency);
    }

    @Override
    public PartitionedHistogram<T> current() {
        return rolling.current();
    }

    @Override
    public PartitionedHistogram<T> current(long nanoTime) {
        return rolling.current(nanoTime);
    }

    @Override
    public IntervalIterator<PartitionedHistogram<T>> intervals() {
        return rolling.intervals();
    }

    @Override
    public IntervalIterator<PartitionedHistogram<T>> intervals(long nanoTime) {
        return rolling.intervals(nanoTime);
    }
}