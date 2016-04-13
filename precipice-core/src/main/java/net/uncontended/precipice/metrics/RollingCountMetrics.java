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

public class RollingCountMetrics<T extends Enum<T>> extends AbstractMetrics<T> implements WritableCounts<T>,
        Rolling<PartitionedCount<T>> {

    private final PartitionedCount<T> totalCounter;
    private final PartitionedCount<T> noOpCounter;
    private final CircularBuffer<PartitionedCount<T>> buffer;
    private final Allocator factory;
    private final Clock clock;

    public RollingCountMetrics(Class<T> type) {
        this(type, Counters.longAdder());
    }

    public RollingCountMetrics(Class<T> type, Allocator factory) {
        this(type, factory, true, (int) TimeUnit.MINUTES.toSeconds(15), 1, TimeUnit.SECONDS, new SystemTime());
    }

    public RollingCountMetrics(Class<T> type, int slotsToTrack, long resolution, TimeUnit slotUnit) {
        this(type, slotsToTrack, resolution, slotUnit, new SystemTime());
    }

    public RollingCountMetrics(Class<T> type, Allocator factory, int slotsToTrack, long resolution, TimeUnit slotUnit) {
        this(type, factory, true, slotsToTrack, resolution, slotUnit, new SystemTime());
    }

    public RollingCountMetrics(Class<T> type, int slotsToTrack, long resolution, TimeUnit slotUnit, Clock clock) {
        this(type, Counters.longAdder(), true, slotsToTrack, resolution, slotUnit, clock);
    }

    public RollingCountMetrics(Class<T> clazz, Allocator factory, boolean trackTotalCounts, int slotsToTrack,
                               long resolution, TimeUnit slotUnit, Clock clock) {
        super(clazz);
        this.clock = clock;
        this.factory = factory;

        long startNanos = clock.nanoTime();

        buffer = new CircularBuffer<>(slotsToTrack, resolution, slotUnit, startNanos);
        if (trackTotalCounts) {
            totalCounter = factory.allocateNew(this.clazz);
        } else {
            totalCounter = new NoOpCounter<>(clazz);
        }
        noOpCounter = new NoOpCounter<>(clazz);
    }

    @Override
    public void add(T metric, long delta) {
        add(metric, delta, clock.nanoTime());
    }

    @Override
    public void add(T metric, long delta, long nanoTime) {
        totalCounter.add(metric, delta);
        PartitionedCount<T> currentMetricCounter = buffer.getSlot(nanoTime);
        if (currentMetricCounter == null) {
            PartitionedCount<T> newCounter = factory.allocateNew(clazz);
            currentMetricCounter = buffer.putOrGet(nanoTime, newCounter);
        }
        if (currentMetricCounter != null) {
            currentMetricCounter.add(metric, delta);
        }
    }

    public PartitionedCount<T> totalCounter() {
        return totalCounter;
    }

    @Override
    public PartitionedCount<T> currentInterval() {
        return currentInterval(clock.nanoTime());
    }

    @Override
    public PartitionedCount<T> currentInterval(long nanoTime) {
        PartitionedCount<T> counter = buffer.getSlot(nanoTime);
        return counter != null ? counter : noOpCounter;
    }

    @Override
    public IntervalIterator<PartitionedCount<T>> intervals() {
        return intervals(clock.nanoTime());
    }

    @Override
    public IntervalIterator<PartitionedCount<T>> intervals(long nanoTime) {
        return buffer.intervals(nanoTime, noOpCounter);
    }
}
