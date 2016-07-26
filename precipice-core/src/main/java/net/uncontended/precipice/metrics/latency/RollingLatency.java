package net.uncontended.precipice.metrics.latency;

import net.uncontended.precipice.metrics.AbstractMetrics;
import net.uncontended.precipice.metrics.IntervalIterator;
import net.uncontended.precipice.metrics.Rolling;
import net.uncontended.precipice.metrics.tools.Allocator;
import net.uncontended.precipice.metrics.tools.CircularBuffer;
import net.uncontended.precipice.metrics.tools.RollingMetrics;
import net.uncontended.precipice.time.SystemTime;

public class RollingLatency<T extends Enum<T>> extends AbstractMetrics<T> implements WritableLatency<T>, Rolling<PartitionedHistogram<T>> {

    private final RollingMetrics<PartitionedHistogram<T>> rolling;
    private final NoOpLatency<T> noOpLatency;

    public RollingLatency(Class<T> clazz, int buckets, long nanosPerBucket) {
        this(Latency.atomicHDRHistogram(clazz), buckets, nanosPerBucket);
    }

    public RollingLatency(Allocator<PartitionedHistogram<T>> allocator, int buckets, long nanosPerBucket) {
        this(new RollingMetrics<PartitionedHistogram<T>>(allocator,
                new CircularBuffer<PartitionedHistogram<T>>(buckets, nanosPerBucket, System.nanoTime()),
                new SystemTime()));
    }

    public RollingLatency(RollingMetrics<PartitionedHistogram<T>> rolling) {
        super(rolling.current().getMetricClazz());
        this.rolling = rolling;
        this.noOpLatency = new NoOpLatency<>(getMetricClazz());
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
        return rolling.intervalsWithDefault(noOpLatency);
    }

    @Override
    public IntervalIterator<PartitionedHistogram<T>> intervals(long nanoTime) {
        return rolling.intervalsWithDefault(nanoTime, noOpLatency);
    }
}
