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

public class RollingCountMetrics<T extends Enum<T>> extends AbstractMetrics<T> implements CountMetrics<T>,
        Rolling<CountMetrics<T>> {

    private final CountMetrics<T> totalCounter;
    private final CountMetrics<T> noOpCounter;
    private final CircularBuffer<CountMetrics<T>> buffer;
    private final CounterFactory factory = Counters.adding();
    private final int intervalsToBuffer;
    private final Clock clock;

    public RollingCountMetrics(Class<T> type) {
        this(type, Counters.adding());
    }

    public RollingCountMetrics(Class<T> type, CounterFactory factory) {
        this(type, factory, (int) TimeUnit.MINUTES.toSeconds(15), 1, TimeUnit.SECONDS, new SystemTime());
    }

    public RollingCountMetrics(Class<T> type, int slotsToTrack, long resolution, TimeUnit slotUnit) {
        this(type, slotsToTrack, resolution, slotUnit, new SystemTime());
    }

    public RollingCountMetrics(Class<T> type, CounterFactory factory, int slotsToTrack, long resolution, TimeUnit slotUnit) {
        this(type, factory, slotsToTrack, resolution, slotUnit, new SystemTime());
    }

    public RollingCountMetrics(Class<T> type, int slotsToTrack, long resolution, TimeUnit slotUnit, Clock clock) {
        this(type, Counters.adding(), slotsToTrack, resolution, slotUnit, clock);
    }

    public RollingCountMetrics(Class<T> clazz, CounterFactory factory, int slotsToTrack, long resolution,
                               TimeUnit slotUnit, Clock clock) {
        super(clazz);
        this.intervalsToBuffer = slotsToTrack;
        this.clock = clock;
        long startNanos = clock.nanoTime();

        buffer = new CircularBuffer<>(slotsToTrack, resolution, slotUnit, startNanos);
        totalCounter = factory.newCounter(this.clazz, startNanos);
        noOpCounter = new NoOpCounter<>(clazz);
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
            currentMetricCounter = buffer.putOrGet(nanoTime, factory.newCounter(clazz, nanoTime));
        }
        if (currentMetricCounter != null) {
            currentMetricCounter.add(metric, delta);
        }
    }

    @Override
    public long getCount(T metric) {
        return totalCounter.getCount(metric);
    }

    public long getCountForPeriod(T metric, long timePeriod, TimeUnit timeUnit) {
        return getCountForPeriod(metric, timePeriod, timeUnit, clock.nanoTime());
    }

    public long getCountForPeriod(T metric, long timePeriod, TimeUnit timeUnit, long nanoTime) {
        Iterable<CountMetrics<T>> slots = buffer.valuesForTimePeriod(timePeriod, timeUnit, nanoTime, noOpCounter);

        long count = 0;
        for (CountMetrics<T> metricCounter : slots) {
            count += metricCounter.getCount(metric);
        }
        return count;
    }

    @Override
    public CountMetrics<T> currentInterval() {
        return currentInterval(clock.nanoTime());
    }

    @Override
    public CountMetrics<T> currentInterval(long nanoTime) {
        CountMetrics<T> counter = buffer.getSlot(nanoTime);
        return counter != null ? counter : noOpCounter;
    }

    @Override
    public IntervalIterable<CountMetrics<T>> intervalsForPeriod(long timePeriod, TimeUnit timeUnit) {
        return intervalsForPeriod(timePeriod, timeUnit, clock.nanoTime());
    }

    @Override
    public IntervalIterable<CountMetrics<T>> intervalsForPeriod(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        return buffer.intervalsForTimePeriod(timePeriod, timeUnit, nanoTime, noOpCounter);
    }

    @Override
    public IntervalIterable<CountMetrics<T>> intervals() {
        return intervals(clock.nanoTime());
    }

    @Override
    public IntervalIterable<CountMetrics<T>> intervals(long nanoTime) {
        return buffer.intervals(this.intervalsToBuffer, nanoTime, noOpCounter);
    }
}
