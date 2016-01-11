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

import net.uncontended.precipice.SuperStatusInterface;
import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class SWActionMetrics<T extends Enum<T> & SuperStatusInterface> implements ActionMetrics<T>, BackgroundTask {

    private final MetricCounter<T> totalCounter;
    private final MetricCounter<T> noOpCounter;
    private final SWCircularBuffer<MetricCounter<T>> buffer;
    private final int slotsToTrack;
    private final long millisecondsPerSlot;
    private final Clock systemTime;
    private final Class<T> type;

    public SWActionMetrics(Class<T> type) {
        this(type, 3600, 1, TimeUnit.SECONDS);
    }

    public SWActionMetrics(Class<T> type, int slotsToTrack, long resolution, TimeUnit slotUnit) {
        this(type, slotsToTrack, resolution, slotUnit, new SystemTime());
    }

    public SWActionMetrics(Class<T> type, int slotsToTrack, long resolution, TimeUnit slotUnit, Clock systemTime) {
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
        this.type = type;
        totalCounter = new MetricCounter<>(this.type);
        noOpCounter = MetricCounter.noOpCounter(type);
        buffer = new SWCircularBuffer<>(slotsToTrack, resolution, slotUnit, startTime, new MetricCounter<>(type));
    }

    @Override
    public void incrementMetricCount(T metric) {
        incrementMetricCount(metric, -1);
    }

    @Override
    public void incrementMetricCount(T metric, long nanoTime) {
        totalCounter.incrementMetric(metric);
        MetricCounter<T> currentMetricCounter = buffer.getSlot();
        currentMetricCounter.incrementMetric(metric);
    }

    @Override
    public long getMetricCount(T metric) {
        return totalCounter.getMetricCount(metric);
    }

    @Override
    public long getMetricCountForTotalPeriod(T metric) {
        return getMetricCountForTotalPeriod(metric, systemTime.nanoTime());
    }

    @Override
    public long getMetricCountForTotalPeriod(T metric, long nanoTime) {
        long milliseconds = slotsToTrack * millisecondsPerSlot;
        return getMetricCountForTimePeriod(metric, milliseconds, TimeUnit.MILLISECONDS, nanoTime);
    }

    @Override
    public long getMetricCountForTimePeriod(T metric, long timePeriod, TimeUnit timeUnit) {
        return getMetricCountForTimePeriod(metric, timePeriod, timeUnit, systemTime.nanoTime());
    }

    @Override
    public long getMetricCountForTimePeriod(T metric, long timePeriod, TimeUnit timeUnit, long nanoTime) {
        Iterable<MetricCounter<T>> slots = buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit, nanoTime, noOpCounter);

        long count = 0;
        for (MetricCounter<T> metricCounter : slots) {
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
        Iterable<MetricCounter<T>> counters = buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit, nanoTime,
                noOpCounter);

        long total = 0;
        long notRejectedTotal = 0;
        long failures = 0;
        long rejections = 0;
        for (MetricCounter<T> metricCounter : counters) {
            for (T t : type.getEnumConstants()) {
                long metricCount = metricCounter.getMetricCount(t);
                total += metricCount;

                if (t.isFailure()) {
                    failures += metricCount;
                }

                if (t.isRejected()) {
                    rejections += metricCount;
                } else {
                    notRejectedTotal += metricCount;
                }
            }
        }
        return new HealthSnapshot(total, notRejectedTotal, failures, rejections);
    }

    @Override
    public Map<Object, Object> snapshot(long timePeriod, TimeUnit timeUnit) {
        return Snapshot.generate(totalCounter, buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit,
                systemTime.nanoTime(), noOpCounter));
    }

    @Override
    public Iterable<MetricCounter<T>> metricCounterIterable(long timePeriod, TimeUnit timeUnit) {
        return buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit, systemTime.nanoTime(), noOpCounter);
    }

    @Override
    public MetricCounter totalCountMetricCounter() {
        return totalCounter;
    }

    @Override
    public void tick(long nanoTime) {
        buffer.put(nanoTime, new MetricCounter<T>(type));
    }
}
