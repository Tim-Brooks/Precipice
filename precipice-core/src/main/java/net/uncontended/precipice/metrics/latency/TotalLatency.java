package net.uncontended.precipice.metrics.latency;

import net.uncontended.precipice.metrics.AbstractMetrics;
import org.HdrHistogram.Histogram;

public class TotalLatency<T extends Enum<T>> extends AbstractMetrics<T> implements WritableLatency<T>, PartitionedLatency<T> {

    private final PartitionedLatency<T> latency;

    public TotalLatency(PartitionedLatency<T> latency) {
        super(latency.getMetricClazz());
        this.latency = latency;
    }

    public TotalLatency(Class<T> clazz) {
        super(clazz);
        latency = new AtomicHistogram<>(clazz);
    }

    @Override
    public void record(T result, long number, long nanoLatency) {
        latency.record(result, number, nanoLatency);
    }

    @Override
    public Histogram getHistogram(T metric) {
        return latency.getHistogram(metric);
    }

    @Override
    public long getValueAtPercentile(T metric, double percentile) {
        return latency.getValueAtPercentile(metric, percentile);
    }

    @Override
    public boolean isHDR() {
        return latency.isHDR();
    }

    @Override
    public void reset() {
        latency.reset();
    }

    @Override
    public void write(T result, long number, long nanoLatency, long nanoTime) {
        record(result, number, nanoLatency);
    }
}
