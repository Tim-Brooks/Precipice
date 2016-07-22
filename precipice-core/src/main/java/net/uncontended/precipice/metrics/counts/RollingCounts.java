package net.uncontended.precipice.metrics.counts;

import net.uncontended.precipice.metrics.AbstractMetrics;
import net.uncontended.precipice.metrics.IntervalIterator;
import net.uncontended.precipice.metrics.Rolling;
import net.uncontended.precipice.metrics.tools.Allocator;
import net.uncontended.precipice.metrics.tools.CircularBuffer;
import net.uncontended.precipice.metrics.tools.RollingMetrics;
import net.uncontended.precipice.time.SystemTime;

import java.util.concurrent.TimeUnit;

public class RollingCounts<T extends Enum<T>> extends AbstractMetrics<T> implements WritableCounts<T>, Rolling<PartitionedCount<T>> {

    private final RollingMetrics<PartitionedCount<T>> rolling;

    // Need: Clazz or Allocator
    // Optional: Clock, Bucket count and resolution

    public RollingCounts(Class<T> clazz) {
        this(clazz, Counters.longAdder(clazz));
    }

    public RollingCounts(Allocator<PartitionedCount<T>> allocator) {
        this(allocator.allocateNew().getMetricClazz(), allocator);
    }

    public RollingCounts(Class<T> clazz, Allocator<PartitionedCount<T>> allocator) {
        super(clazz);
        SystemTime clock = new SystemTime();
        CircularBuffer<PartitionedCount<T>> buffer = new CircularBuffer<>(60, TimeUnit.SECONDS.toNanos(1), clock.nanoTime());
        rolling = new RollingMetrics<PartitionedCount<T>>(allocator, buffer, clock);
    }

    @Override
    public void write(T metric, long number, long nanoTime) {
        current(nanoTime).add(metric, number);
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
