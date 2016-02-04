package net.uncontended.precipice.backpressure;

import net.uncontended.precipice.Result;
import net.uncontended.precipice.metrics.CountMetrics;
import net.uncontended.precipice.metrics.HealthSnapshot;
import net.uncontended.precipice.metrics.MetricCounter;

import java.util.concurrent.TimeUnit;

public class HealthGauge<T extends Enum<T> & Result> {

    private final CountMetrics<T> metrics;
    private final Class<T> type;

    public HealthGauge(CountMetrics<T> metrics) {
        this.metrics = metrics;
        type = metrics.getMetricType();
    }

    public HealthSnapshot thing(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        Iterable<MetricCounter<T>> counters = metrics.metricCounterIterable(timePeriod, timeUnit, nanoTime);

        long total = 0;
        long failures = 0;
        for (MetricCounter<T> metricCounter : counters) {
            for (T t : type.getEnumConstants()) {
                long metricCount = metricCounter.getMetricCount(t);
                total += metricCount;

                if (t.isFailure()) {
                    failures += metricCount;
                }
            }
        }
        return new HealthSnapshot(total, 0, failures, 0);
    }
}
