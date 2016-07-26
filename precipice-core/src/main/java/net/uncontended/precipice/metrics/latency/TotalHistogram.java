package net.uncontended.precipice.metrics.latency;

import net.uncontended.precipice.metrics.AbstractMetrics;
import org.HdrHistogram.Histogram;

public class TotalHistogram<T extends Enum<T>> extends AbstractMetrics<T> implements WritableLatency<T>, PartitionedHistogram<T> {

    private final AtomicHistogram<T> histogram;

    public TotalHistogram(AtomicHistogram<T> histogram) {
        super(histogram.getMetricClazz());
        this.histogram = histogram;
    }

    public TotalHistogram(Class<T> clazz) {
        super(clazz);
        histogram = new AtomicHistogram<>(clazz);
    }

    @Override
    public void record(T result, long number, long nanoLatency) {
        histogram.record(result, number, nanoLatency);
    }

    @Override
    public Histogram getHistogram(T metric) {
        return histogram.getHistogram(metric);
    }

    @Override
    public void reset() {
        histogram.reset();
    }

    @Override
    public void write(T result, long number, long nanoLatency, long nanoTime) {
        record(result, number, nanoLatency);
    }
}
