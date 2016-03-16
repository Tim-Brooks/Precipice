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

import java.util.concurrent.TimeUnit;

public class RollingCountMetrics<T extends Enum<T>> implements CountMetrics<T> {

    private final MetricCounter<T> totalCounter;
    private final CountMetrics<T> noOpCounter;
    private final CircularBuffer<CountMetrics<T>> buffer;
    private final Clock clock;
    private final Class<T> type;

    public RollingCountMetrics(Class<T> type) {
        this(type, (int) TimeUnit.MINUTES.toSeconds(15), 1, TimeUnit.SECONDS);
    }

    public RollingCountMetrics(Class<T> type, int slotsToTrack, long resolution, TimeUnit slotUnit) {
        this(type, slotsToTrack, resolution, slotUnit, new SystemTime());
    }

    public RollingCountMetrics(Class<T> type, int slotsToTrack, long resolution, TimeUnit slotUnit, Clock clock) {
        this.clock = clock;
        long millisecondsPerSlot = slotUnit.toMillis(resolution);
        if (millisecondsPerSlot < 0) {
            throw new IllegalArgumentException(String.format("Too low of resolution. %s milliseconds per slot is the " +
                    "lowest valid resolution", Integer.MAX_VALUE));
        }
        if (100 > millisecondsPerSlot) {
            throw new IllegalArgumentException(String.format("Too low of resolution: [%s milliseconds]. 100 " +
                    "milliseconds is the minimum resolution.", millisecondsPerSlot));
        }

        long startNanos = clock.nanoTime();

        this.type = type;
        totalCounter = new MetricCounter<>(this.type);
        noOpCounter = new NoOpMetricCounter<>(type);
        buffer = new CircularBuffer<>(slotsToTrack, resolution, slotUnit, startNanos);
    }

    @Override
    public void add(T metric, long delta) {
        add(metric, delta, clock.nanoTime());
    }

    @Override
    public void add(T metric, long delta, long nanoTime) {
        totalCounter.add(metric, delta);
        CountMetrics<T> currentMetricCounter = buffer.getSlot(nanoTime);
        if (currentMetricCounter == null) {
            currentMetricCounter = buffer.putOrGet(nanoTime, new MetricCounter<>(type));
        }
        if (currentMetricCounter != null) {
            currentMetricCounter.add(metric, delta);
        }
    }

    @Override
    public long getCount(T metric) {
        return totalCounter.getCount(metric);
    }

    public long getMetricCountForPeriod(T metric, long timePeriod, TimeUnit timeUnit) {
        return getMetricCountForPeriod(metric, timePeriod, timeUnit, clock.nanoTime());
    }

    public long getMetricCountForPeriod(T metric, long timePeriod, TimeUnit timeUnit, long nanoTime) {
        Iterable<CountMetrics<T>> slots = buffer.activeSlotsForTimePeriod(timePeriod, timeUnit, nanoTime, noOpCounter);

        long count = 0;
        for (CountMetrics<T> metricCounter : slots) {
            count += metricCounter.getCount(metric);
        }
        return count;
    }

    public Iterable<CountMetrics<T>> metricCounters(long timePeriod, TimeUnit timeUnit) {
        return metricCounters(timePeriod, timeUnit, clock.nanoTime());
    }

    public Iterable<CountMetrics<T>> metricCounters(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        return buffer.activeSlotsForTimePeriod(timePeriod, timeUnit, nanoTime, noOpCounter);
    }

    @Override
    public Class<T> getMetricType() {
        return type;
    }
}
