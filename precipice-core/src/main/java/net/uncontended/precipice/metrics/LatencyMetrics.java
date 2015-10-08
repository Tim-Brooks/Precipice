package net.uncontended.precipice.metrics;

/**
 * Created by timbrooks on 10/8/15.
 */
public interface LatencyMetrics {
    void recordLatency(long nanoLatency);

    void recordLatency(long nanoLatency, long nanoTime);
}
