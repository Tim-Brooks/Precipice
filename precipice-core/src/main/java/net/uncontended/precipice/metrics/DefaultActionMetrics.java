/*
 * Copyright 2014 Timothy Brooks
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

package net.uncontended.precipice.metrics;

import net.uncontended.precipice.utils.SystemTime;
import org.HdrHistogram.AtomicHistogram;
import org.HdrHistogram.Histogram;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DefaultActionMetrics implements ActionMetrics {

    private final CircularBuffer<MetricCounter> buffer;
    private final Histogram latency = new AtomicHistogram(TimeUnit.HOURS.toNanos(1), 2);
    private final MetricCounter totalCounter = new MetricCounter();
    private final int slotsToTrack;
    private final long millisecondsPerSlot;
    private final SystemTime systemTime;

    public DefaultActionMetrics() {
        this(3600, 1, TimeUnit.SECONDS);
    }

    public DefaultActionMetrics(int slotsToTrack, long resolution, TimeUnit slotUnit) {
        this(slotsToTrack, resolution, slotUnit, new SystemTime());
    }

    public DefaultActionMetrics(int slotsToTrack, long resolution, TimeUnit slotUnit, SystemTime systemTime) {
        this.systemTime = systemTime;
        this.millisecondsPerSlot = slotUnit.toMillis(resolution);
        if (millisecondsPerSlot < 0) {
            throw new IllegalArgumentException(String.format("Too low of resolution. %s milliseconds per slot is the " +
                    "lowest valid resolution", Integer.MAX_VALUE));
        } else if (100 > millisecondsPerSlot) {
            throw new IllegalArgumentException(String.format("Too low of resolution: [%s milliseconds]. 100 " +
                    "milliseconds is the minimum resolution.", millisecondsPerSlot));
        }

        long startTime = systemTime.nanoTime();
        this.slotsToTrack = slotsToTrack;
        this.buffer = new CircularBuffer<>(slotsToTrack, resolution, slotUnit, startTime);
    }

    @Override
    public void incrementMetricCount(Metric metric) {
        incrementMetricCount(metric, systemTime.nanoTime());
    }

    @Override
    public void incrementMetricCount(Metric metric, long nanoTime) {
        incrementMetricAndRecordLatency(metric, -1L, nanoTime);
    }

    @Override
    public void incrementMetricAndRecordLatency(Metric metric, long nanoLatency, long nanoTime) {
        totalCounter.incrementMetric(metric);
        MetricCounter currentMetricCounter = buffer.getSlot(nanoTime);
        if (currentMetricCounter == null) {
            currentMetricCounter = buffer.putOrGet(nanoTime, new MetricCounter());
        }
        currentMetricCounter.incrementMetric(metric);
        recordLatency(nanoLatency);
    }

    @Override
    public long getMetricCount(Metric metric) {
        return totalCounter.getMetric(metric).longValue();
    }

    @Override
    public long getMetricCountForTotalPeriod(Metric metric) {
        return getMetricCountForTotalPeriod(metric, systemTime.nanoTime());
    }

    @Override
    public long getMetricCountForTotalPeriod(Metric metric, long nanoTime) {
        long milliseconds = slotsToTrack * millisecondsPerSlot;
        return getMetricCountForTimePeriod(metric, milliseconds, TimeUnit.MILLISECONDS, nanoTime);
    }

    @Override
    public long getMetricCountForTimePeriod(Metric metric, long timePeriod, TimeUnit timeUnit) {
        return getMetricCountForTimePeriod(metric, timePeriod, timeUnit, systemTime.nanoTime());
    }

    @Override
    public long getMetricCountForTimePeriod(Metric metric, long timePeriod, TimeUnit timeUnit, long nanoTime) {
        Iterable<MetricCounter> slots = buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit, nanoTime);

        long count = 0;
        for (MetricCounter metricCounter : slots) {
            if (metricCounter != null) {
                count = count + metricCounter.getMetric(metric).longValue();
            }
        }
        return count;
    }

    @Override
    public HealthSnapshot healthSnapshot(long timePeriod, TimeUnit timeUnit) {
        return healthSnapshot(timePeriod, timeUnit, systemTime.nanoTime());
    }

    @Override
    public HealthSnapshot healthSnapshot(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        Iterable<MetricCounter> slots = buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit, nanoTime);

        long total = 0;
        long failures = 0;
        long rejections = 0;
        for (MetricCounter metricCounter : slots) {
            if (metricCounter != null) {
                long successes = metricCounter.getMetric(Metric.SUCCESS).longValue();
                long errors = metricCounter.getMetric(Metric.ERROR).longValue();
                long timeouts = metricCounter.getMetric(Metric.TIMEOUT).longValue();
                long maxConcurrency = metricCounter.getMetric(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED).longValue();
                long circuitOpen = metricCounter.getMetric(Metric.CIRCUIT_OPEN).longValue();
                long queueFull = metricCounter.getMetric(Metric.QUEUE_FULL).longValue();
                long slotTotal = successes + errors + timeouts + maxConcurrency + circuitOpen + queueFull;

                total = total + slotTotal;
                failures = failures + errors + timeouts;
                rejections = rejections + circuitOpen + queueFull + maxConcurrency;
            }
        }
        return new HealthSnapshot(total, failures, rejections);
    }

    @Override
    public Map<Object, Object> snapshot(long timePeriod, TimeUnit timeUnit) {
        return Snapshot.generate(buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit, systemTime.nanoTime()));

    }

    private void recordLatency(long nanoDuration) {
        if (nanoDuration != -1) {
            if (nanoDuration < TimeUnit.HOURS.toNanos(1)) {
                latency.recordValue(nanoDuration);
            } else {
                latency.recordValue(TimeUnit.HOURS.toNanos(1));
            }
        }
    }
}
