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

package net.uncontended.precipice.metrics.latency;

import net.uncontended.precipice.metrics.AbstractMetrics;
import net.uncontended.precipice.metrics.tools.FlipControl;
import net.uncontended.precipice.metrics.tools.Recorder;
import net.uncontended.precipice.time.Clock;

public class LatencyRecorder<T extends Enum<T>> extends AbstractMetrics<T> implements WritableLatency<T>, Recorder<PartitionedLatency<T>> {

    private final Object lock = new Object();
    private final FlipControl<PartitionedLatency<T>> flipControl;
    private final Clock clock;
    private PartitionedLatency<T> inactive;
    private long intervalStart;

    public LatencyRecorder(PartitionedLatency<T> active, PartitionedLatency<T> inactive, FlipControl<PartitionedLatency<T>> flipControl, Clock clock) {
        super(active.getMetricClazz());
        this.flipControl = flipControl;
        this.clock = clock;
        this.flipControl.flip(active);
        this.inactive = inactive;
        this.intervalStart = clock.nanoTime();
    }

    @Override
    public void write(T result, long number, long nanoLatency, long nanoTime) {
        long permit = flipControl.startRecord();
        try {
            flipControl.active().record(result, number, nanoLatency);
        } finally {
            flipControl.endRecord(permit);
        }
    }

    @Override
    public PartitionedLatency<T> activeInterval() {
        return flipControl.active();
    }

    @Override
    public long activeIntervalStart() {
        synchronized (lock) {
            return intervalStart;
        }
    }

    @Override
    public PartitionedLatency<T> captureInterval() {
        return captureInterval(clock.nanoTime());
    }

    @Override
    public PartitionedLatency<T> captureInterval(long nanotime) {
        inactive.reset();
        return captureInterval(inactive, nanotime);
    }

    @Override
    public PartitionedLatency<T> captureInterval(PartitionedLatency<T> newInterval) {
        return captureInterval(newInterval, clock.nanoTime());
    }

    @Override
    public PartitionedLatency<T> captureInterval(PartitionedLatency<T> newInterval, long nanoTime) {
        synchronized (lock) {
            PartitionedLatency<T> newlyInactive = flipControl.flip(newInterval);
            inactive = newlyInactive;
            intervalStart = nanoTime;
            return newlyInactive;
        }
    }

    public static <T extends Enum<T>> LatencyRecorderBuilder<T> builder(Class<T> clazz) {
        return new LatencyRecorderBuilder<>(clazz);
    }
}
