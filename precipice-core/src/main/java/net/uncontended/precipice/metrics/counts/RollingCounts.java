package net.uncontended.precipice.metrics.counts;

import net.uncontended.precipice.metrics.AbstractMetrics;
import net.uncontended.precipice.metrics.IntervalIterator;
import net.uncontended.precipice.metrics.Rolling;
import net.uncontended.precipice.metrics.tools.CircularBuffer;
import net.uncontended.precipice.metrics.tools.RollingMetrics;
import net.uncontended.precipice.time.SystemTime;

public class RollingCounts<T extends Enum<T>> extends AbstractMetrics<T> implements WritableCounts<T>, Rolling<PartitionedCount<T>> {

    private final NoOpCounter<T> noOpCounter;

    private final RollingMetrics<PartitionedCount<T>> rolling;

    // Need: Clazz, Bucket count and resolution
    // Optional: Allocator, Clock

    public RollingCounts(Class<T> clazz, int buckets, long nanosPerBucket) {
        this(new RollingMetrics<PartitionedCount<T>>(Counters.longAdder(clazz),
                new CircularBuffer<PartitionedCount<T>>(buckets, nanosPerBucket, System.nanoTime()),
                SystemTime.getInstance()));
    }

    public RollingCounts(RollingMetrics<PartitionedCount<T>> rolling) {
        super(rolling.current().getMetricClazz());
        this.rolling = rolling;
        this.noOpCounter = new NoOpCounter<>(getMetricClazz());
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
        return rolling.intervalsWithDefault(noOpCounter);
    }

    @Override
    public IntervalIterator<PartitionedCount<T>> intervals(long nanoTime) {
        return rolling.intervalsWithDefault(nanoTime, noOpCounter);
    }

    public static <V extends Enum<V>> RollingCountsBuilder<V> builder(Class<V> clazz) {
        return new RollingCountsBuilder<>(clazz);

    }
}
