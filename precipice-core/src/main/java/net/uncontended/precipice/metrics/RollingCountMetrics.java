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

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class RollingCountMetrics<T extends Enum<T>> extends AbstractMetrics<T> implements WritableCountMetrics<T>,
        Rolling<ReadableCountMetrics<T>> {

    private final ReadableCountMetrics<T> totalCounter;
    private final ReadableCountMetrics<T> noOpCounter;
    private final CircularBuffer<ReadableCountMetrics<T>> buffer;
    private final CounterFactory factory;
    private final int intervalsToBuffer;
    private final Clock clock;

    public RollingCountMetrics(Class<T> type) {
        this(type, Counters.adding());
    }

    public RollingCountMetrics(Class<T> type, CounterFactory factory) {
        this(type, factory, true, (int) TimeUnit.MINUTES.toSeconds(15), 1, TimeUnit.SECONDS, new SystemTime());
    }

    public RollingCountMetrics(Class<T> type, int slotsToTrack, long resolution, TimeUnit slotUnit) {
        this(type, slotsToTrack, resolution, slotUnit, new SystemTime());
    }

    public RollingCountMetrics(Class<T> type, CounterFactory factory, int slotsToTrack, long resolution, TimeUnit slotUnit) {
        this(type, factory, true, slotsToTrack, resolution, slotUnit, new SystemTime());
    }

    public RollingCountMetrics(Class<T> type, int slotsToTrack, long resolution, TimeUnit slotUnit, Clock clock) {
        this(type, Counters.adding(), true, slotsToTrack, resolution, slotUnit, clock);
    }

    public RollingCountMetrics(Class<T> clazz, CounterFactory factory, boolean trackTotalCounts, int slotsToTrack,
                               long resolution, TimeUnit slotUnit, Clock clock) {
        super(clazz);
        this.intervalsToBuffer = slotsToTrack;
        this.clock = clock;
        this.factory = factory;

        long startNanos = clock.nanoTime();

        buffer = new CircularBuffer<>(slotsToTrack, resolution, slotUnit, startNanos);
        totalCounter = trackTotalCounts ? factory.newCounter(this.clazz) : new NoOpCounter<>(clazz);
        noOpCounter = new NoOpCounter<>(clazz);
    }

    @Override
    public void add(T metric, long delta) {
        add(metric, delta, clock.nanoTime());
    }

    @Override
    public void add(T metric, long delta, long nanoTime) {
        totalCounter.add(metric, delta);
        ReadableCountMetrics<T> currentMetricCounter = buffer.getSlot(nanoTime);
        if (currentMetricCounter == null) {
            currentMetricCounter = buffer.putOrGet(nanoTime, factory.newCounter(clazz));
        }
        if (currentMetricCounter != null) {
            currentMetricCounter.add(metric, delta);
        }
    }

    public ReadableCountMetrics<T> totalCounter() {
        return totalCounter;
    }


    public long getCountForPeriod(T metric, long timePeriod, TimeUnit timeUnit) {
        return getCountForPeriod(metric, timePeriod, timeUnit, clock.nanoTime());
    }

    public long getCountForPeriod(T metric, long timePeriod, TimeUnit timeUnit, long nanoTime) {
        Iterator<ReadableCountMetrics<T>> slots = buffer.valuesForTimePeriod(timePeriod, timeUnit, nanoTime, noOpCounter);
        return Accumulator.countForPeriod(slots, metric);
    }

    @Override
    public ReadableCountMetrics<T> currentInterval() {
        return currentInterval(clock.nanoTime());
    }

    @Override
    public ReadableCountMetrics<T> currentInterval(long nanoTime) {
        ReadableCountMetrics<T> counter = buffer.getSlot(nanoTime);
        return counter != null ? counter : noOpCounter;
    }

    @Override
    public IntervalIterator<ReadableCountMetrics<T>> intervalsForPeriod(long timePeriod, TimeUnit timeUnit) {
        return intervalsForPeriod(timePeriod, timeUnit, clock.nanoTime());
    }

    @Override
    public IntervalIterator<ReadableCountMetrics<T>> intervalsForPeriod(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        return buffer.intervalsForTimePeriod(timePeriod, timeUnit, nanoTime, noOpCounter);
    }

    @Override
    public IntervalIterator<ReadableCountMetrics<T>> intervals() {
        return intervals(clock.nanoTime());
    }

    @Override
    public IntervalIterator<ReadableCountMetrics<T>> intervals(long nanoTime) {
        return buffer.intervals(this.intervalsToBuffer, nanoTime, noOpCounter);
    }
}
