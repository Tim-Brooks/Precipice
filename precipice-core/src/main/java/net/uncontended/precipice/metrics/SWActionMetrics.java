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

package net.uncontended.precipice.metrics;

import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SWActionMetrics implements ActionMetrics {

    private final MetricCounter totalCounter = new MetricCounter();
    private final SWCircularBuffer<MetricCounter> buffer;
    private final int slotsToTrack;
    private final long millisecondsPerSlot;
    private final Clock systemTime;

    public SWActionMetrics() {
        this(3600, 1, TimeUnit.SECONDS);
    }

    public SWActionMetrics(int slotsToTrack, long resolution, TimeUnit slotUnit) {
        this(slotsToTrack, resolution, slotUnit, new SystemTime());
    }

    public SWActionMetrics(int slotsToTrack, long resolution, TimeUnit slotUnit, Clock systemTime) {
        this.systemTime = systemTime;
        millisecondsPerSlot = slotUnit.toMillis(resolution);
        if (millisecondsPerSlot < 0) {
            throw new IllegalArgumentException(String.format("Too low of resolution. %s milliseconds per slot is the " +
                    "lowest valid resolution", Integer.MAX_VALUE));
        }
        if (100 > millisecondsPerSlot) {
            throw new IllegalArgumentException(String.format("Too low of resolution: [%s milliseconds]. 100 " +
                    "milliseconds is the minimum resolution.", millisecondsPerSlot));
        }

        long startTime = systemTime.nanoTime();
        this.slotsToTrack = slotsToTrack;
        buffer = new SWCircularBuffer<MetricCounter>(slotsToTrack, resolution, slotUnit, startTime);
    }

    @Override
    public void incrementMetricCount(Metric metric) {
        incrementMetricCount(metric, -1);
    }

    @Override
    public void incrementMetricCount(Metric metric, long nanoTime) {
        totalCounter.incrementMetric(metric);
        MetricCounter currentMetricCounter = buffer.getSlot();
        currentMetricCounter.incrementMetric(metric);
    }

    @Override
    public long getMetricCount(Metric metric) {
        return totalCounter.getMetricCount(metric);
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
        Iterable<MetricCounter> slots = buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit, nanoTime, MetricCounter.NO_OP_COUNTER);

        long count = 0;
        for (MetricCounter metricCounter : slots) {
            count += metricCounter.getMetricCount(metric);
        }
        return count;
    }

    @Override
    public HealthSnapshot healthSnapshot(long timePeriod, TimeUnit timeUnit) {
        return healthSnapshot(timePeriod, timeUnit, systemTime.nanoTime());
    }

    @Override
    public HealthSnapshot healthSnapshot(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        Iterable<MetricCounter> counters = buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit, nanoTime,
                MetricCounter.NO_OP_COUNTER);

        long total = 0;
        long notRejectedTotal = 0;
        long failures = 0;
        long rejections = 0;
        for (MetricCounter metricCounter : counters) {
            long successes = metricCounter.getMetricCount(Metric.SUCCESS);
            long errors = metricCounter.getMetricCount(Metric.ERROR);
            long timeouts = metricCounter.getMetricCount(Metric.TIMEOUT);
            long maxConcurrency = metricCounter.getMetricCount(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED);
            long circuitOpen = metricCounter.getMetricCount(Metric.CIRCUIT_OPEN);
            long queueFull = metricCounter.getMetricCount(Metric.QUEUE_FULL);
            long slotNotRejectedTotal = successes + errors + timeouts;
            long slotTotal = slotNotRejectedTotal + maxConcurrency + circuitOpen + queueFull;

            total += slotTotal;
            notRejectedTotal += slotNotRejectedTotal;
            failures += errors + timeouts;
            rejections += circuitOpen + queueFull + maxConcurrency;
        }
        return new HealthSnapshot(total, notRejectedTotal, failures, rejections);
    }

    @Override
    public Map<Object, Object> snapshot(long timePeriod, TimeUnit timeUnit) {
        return Snapshot.generate(totalCounter, buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit,
                systemTime.nanoTime(), MetricCounter.NO_OP_COUNTER));
    }

    @Override
    public Iterable<MetricCounter> metricCounterIterable(long timePeriod, TimeUnit timeUnit) {
        return buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit, systemTime.nanoTime(),
                MetricCounter.NO_OP_COUNTER);
    }

    @Override
    public MetricCounter totalCountMetricCounter() {
        return totalCounter;
    }

    public void tick(long nanoTime) {
        buffer.put(nanoTime, new MetricCounter());
    }
}
