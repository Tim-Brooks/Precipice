/*
 * Copyright 2015 Timothy Brooks
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

public class RollingLatencyMetrics<T extends Enum<T>> extends AbstractMetrics<T> implements WritableLatencyMetrics<T>,
        Rolling<WritableLatencyMetrics<T>> {

    private final LatencyFactory factory;
    private final Clock clock;
    private final WritableLatencyMetrics<T> totalLatency;
    private final CircularBuffer<WritableLatencyMetrics<T>> buffer;
    private final NoOpLatency<T> noOpLatency;
    private long intervalsToBuffer;

    public RollingLatencyMetrics(Class<T> clazz) {
        this(clazz, Latency.atomicHDRHistogram());
    }

    public RollingLatencyMetrics(Class<T> clazz, LatencyFactory factory) {
        this(clazz, factory, (int) TimeUnit.MINUTES.toSeconds(15), 1, TimeUnit.SECONDS, new SystemTime());
    }

    public RollingLatencyMetrics(Class<T> clazz, int slotsToTrack, long resolution, TimeUnit slotUnit) {
        this(clazz, slotsToTrack, resolution, slotUnit, new SystemTime());
    }

    public RollingLatencyMetrics(Class<T> clazz, LatencyFactory factory, int slotsToTrack, long resolution,
                                 TimeUnit slotUnit) {
        this(clazz, factory, slotsToTrack, resolution, slotUnit, new SystemTime());
    }

    public RollingLatencyMetrics(Class<T> clazz, int slotsToTrack, long resolution, TimeUnit slotUnit, Clock clock) {
        this(clazz, Latency.atomicHDRHistogram(), slotsToTrack, resolution, slotUnit, clock);
    }

    public RollingLatencyMetrics(Class<T> clazz, LatencyFactory factory, int slotsToTrack, long resolution,
                                 TimeUnit slotUnit, Clock clock) {
        super(clazz);
        boolean useTotalCounter = true;
        this.factory = factory;
        this.clock = clock;
        long startNanos = clock.nanoTime();
        this.intervalsToBuffer = slotsToTrack;

        buffer = new CircularBuffer<>(slotsToTrack, resolution, slotUnit, startNanos);
        totalLatency = useTotalCounter ? factory.newLatency(clazz) : new NoOpLatency<>(clazz);
        noOpLatency = new NoOpLatency<>(clazz);
    }

    @Override
    public void record(T metric, long number, long nanoLatency) {
        record(metric, number, nanoLatency, clock.nanoTime());
    }

    @Override
    public void record(T metric, long number, long nanoLatency, long nanoTime) {
        WritableLatencyMetrics<T> latencyMetrics = buffer.getSlot(nanoTime);
        if (latencyMetrics == null) {
            latencyMetrics = buffer.putOrGet(nanoTime, factory.newLatency(clazz));
        }
        if (latencyMetrics != null) {
            latencyMetrics.record(metric, number, nanoLatency, nanoTime);
        }
    }

    @Override
    public WritableLatencyMetrics<T> currentInterval() {
        return currentInterval(clock.nanoTime());
    }

    @Override
    public WritableLatencyMetrics<T> currentInterval(long nanoTime) {
        WritableLatencyMetrics<T> latency = buffer.getSlot(nanoTime);
        return latency != null ? latency : noOpLatency;
    }

    @Override
    public IntervalIterator<WritableLatencyMetrics<T>> intervals() {
        return intervals(clock.nanoTime());
    }

    @Override
    public IntervalIterator<WritableLatencyMetrics<T>> intervals(long nanoTime) {
        return buffer.intervals(nanoTime, noOpLatency);
    }
}
