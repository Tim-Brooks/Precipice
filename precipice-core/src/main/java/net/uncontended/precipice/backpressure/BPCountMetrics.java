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

package net.uncontended.precipice.backpressure;

import net.uncontended.precipice.metrics.CircularBuffer;
import net.uncontended.precipice.metrics.MetricCounter;
import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;

import java.util.concurrent.TimeUnit;

public class BPCountMetrics<T extends Enum<T>> implements BPTotalCountMetrics<T> {

    private final MetricCounter<T> totalCounter;
    private final MetricCounter<T> noOpCounter;
    private final CircularBuffer<MetricCounter<T>> buffer;
    private final Clock systemTime;
    private final Class<T> type;

    public BPCountMetrics(Class<T> type) {
        this(type, 3600, 1, TimeUnit.SECONDS);
    }

    public BPCountMetrics(Class<T> type, int slotsToTrack, long resolution, TimeUnit slotUnit) {
        this(type, slotsToTrack, resolution, slotUnit, new SystemTime());
    }

    public BPCountMetrics(Class<T> type, int slotsToTrack, long resolution, TimeUnit slotUnit, Clock systemTime) {
        this.systemTime = systemTime;
        long millisecondsPerSlot = slotUnit.toMillis(resolution);
        if (millisecondsPerSlot < 0) {
            throw new IllegalArgumentException(String.format("Too low of resolution. %s milliseconds per slot is the " +
                    "lowest valid resolution", Integer.MAX_VALUE));
        }
        if (100 > millisecondsPerSlot) {
            throw new IllegalArgumentException(String.format("Too low of resolution: [%s milliseconds]. 100 " +
                    "milliseconds is the minimum resolution.", millisecondsPerSlot));
        }

        long startTime = systemTime.nanoTime();

        this.type = type;
        totalCounter = new MetricCounter<>(this.type);
        noOpCounter = MetricCounter.noOpCounter(type);
        buffer = new CircularBuffer<>(slotsToTrack, resolution, slotUnit, startTime);
    }

    @Override
    public void incrementMetricCount(T metric) {
        incrementMetricCount(metric, systemTime.nanoTime());
    }

    @Override
    public void incrementMetricCount(T metric, long nanoTime) {
        totalCounter.incrementMetric(metric);
        MetricCounter<T> currentMetricCounter = buffer.getSlot(nanoTime);
        if (currentMetricCounter == null) {
            currentMetricCounter = buffer.putOrGet(nanoTime, new MetricCounter<>(type));
        }
        currentMetricCounter.incrementMetric(metric);
    }

    @Override
    public long getTotalMetricCount(T metric) {
        return totalCounter.getMetricCount(metric);
    }

    public long getMetricCount(T metric, long timePeriod, TimeUnit timeUnit) {
        return getMetricCount(metric, timePeriod, timeUnit, systemTime.nanoTime());
    }

    public long getMetricCount(T metric, long timePeriod, TimeUnit timeUnit, long nanoTime) {
        Iterable<MetricCounter<T>> slots = buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit, nanoTime, noOpCounter);

        long count = 0;
        for (MetricCounter<T> metricCounter : slots) {
            count += metricCounter.getMetricCount(metric);
        }
        return count;
    }

    public Iterable<MetricCounter<T>> metricCounters(long timePeriod, TimeUnit timeUnit) {
        return metricCounters(timePeriod, timeUnit, systemTime.nanoTime());
    }

    public Iterable<MetricCounter<T>> metricCounters(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        return buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit, nanoTime, noOpCounter);
    }

    @Override
    public MetricCounter<T> totalCountMetricCounter() {
        return totalCounter;
    }

    @Override
    public Class<T> getMetricType() {
        return type;
    }
}
