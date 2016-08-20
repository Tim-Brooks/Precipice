/*
 * Copyright 2016 Timothy Brooks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.uncontended.precipice.circuit.experimental;

import net.uncontended.precipice.Failable;
import net.uncontended.precipice.metrics.IntervalIterator;
import net.uncontended.precipice.metrics.Rolling;
import net.uncontended.precipice.metrics.counts.PartitionedCount;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultHealthChecker implements HealthChecker {

    private final long healthRefreshNanos;
    private final long trailingPeriodNanos;
    private final long failureThreshold;
    private final int failurePercentageThreshold;
    private final long sampleSizeThreshold;

    private final AtomicLong lastHealthNanoTime = new AtomicLong();
    private final ArrayList<InternalGauge<?>> gauges = new ArrayList<>();

    private volatile HealthSnapshot health = new HealthSnapshot(0, 0);

    public DefaultHealthChecker(long healthRefreshNanos, long trailingPeriodNanos, long failureThreshold,
                                int failurePercentageThreshold, long sampleSizeThreshold) {
        this.healthRefreshNanos = healthRefreshNanos;
        this.trailingPeriodNanos = trailingPeriodNanos;
        this.failureThreshold = failureThreshold;
        this.failurePercentageThreshold = failurePercentageThreshold;
        this.sampleSizeThreshold = sampleSizeThreshold;
    }

    @Override
    public boolean isHealthy(long nanoTime) {
        HealthSnapshot health = getHealthSnapshot(nanoTime);

        long failures = health.failures;
        int failurePercentage = health.failurePercentage;
        if (failureThreshold < failures || failurePercentageThreshold < failurePercentage &&
                sampleSizeThreshold < health.total) {
            return false;
        }
        return true;
    }

    public void init(long nanoTime) {
        lastHealthNanoTime.set(nanoTime);
    }

    private HealthSnapshot getHealthSnapshot(long currentNanoTime) {
        long lastHealthNanoTime = this.lastHealthNanoTime.get();
        if (currentNanoTime - (lastHealthNanoTime + healthRefreshNanos) > 0) {
            if (this.lastHealthNanoTime.compareAndSet(lastHealthNanoTime, currentNanoTime)) {
                HealthSnapshot newHealth = getHealth(trailingPeriodNanos, currentNanoTime);
                health = newHealth;
                return newHealth;
            }
        }
        return health;
    }

    private synchronized HealthSnapshot getHealth(long trailingPeriodNanos, long nanoTime) {
        long total = 0;
        long failures = 0;

        for (InternalGauge<?> gauge : gauges) {
            gauge.refreshHealth(trailingPeriodNanos, nanoTime);
            total += gauge.total;
            failures += gauge.failures;
        }
        return new HealthSnapshot(total, failures);
    }

    private static class HealthSnapshot {

        private final long total;
        private final long failures;
        private final int failurePercentage;

        private HealthSnapshot(long total, long failures) {
            this.total = total;
            this.failures = failures;
            if (total != 0) {
                failurePercentage = (int) (100 * failures / total);
            } else {
                failurePercentage = 0;
            }
        }
    }

    private static class InternalGauge<Result extends Enum<Result> & Failable> {

        private final Rolling<PartitionedCount<Result>> metrics;
        private final Class<Result> type;
        private long total = 0;
        private long failures = 0;

        private InternalGauge(Rolling<PartitionedCount<Result>> metrics) {
            this.metrics = metrics;
            type = metrics.current().getMetricClazz();
        }

        private void refreshHealth(long trailingPeriodNanos, long nanoTime) {
            total = 0;
            failures = 0;
            IntervalIterator<PartitionedCount<Result>> counters = metrics.intervals(nanoTime);
            counters.limit(trailingPeriodNanos, TimeUnit.NANOSECONDS);

            PartitionedCount<Result> metricCounter;
            while (counters.hasNext()) {
                metricCounter = counters.next();
                for (Result result : type.getEnumConstants()) {
                    long metricCount = metricCounter.getCount(result);
                    total += metricCount;

                    if (result.isFailure()) {
                        failures += metricCount;
                    }
                }
            }
        }
    }
}
