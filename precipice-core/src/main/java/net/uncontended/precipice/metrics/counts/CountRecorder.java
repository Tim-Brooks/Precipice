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

package net.uncontended.precipice.metrics.counts;

import net.uncontended.precipice.metrics.AbstractMetrics;
import net.uncontended.precipice.metrics.tools.FlipControl;
import net.uncontended.precipice.metrics.tools.Recorder;
import net.uncontended.precipice.time.Clock;

public class CountRecorder<T extends Enum<T>> extends AbstractMetrics<T> implements WritableCounts<T>, Recorder<PartitionedCount<T>> {

    private final Object lock = new Object();
    private final FlipControl<PartitionedCount<T>> flipControl;
    private final Clock clock;
    private PartitionedCount<T> inactive;
    private long intervalStart;

    public CountRecorder(PartitionedCount<T> active, PartitionedCount<T> inactive, FlipControl<PartitionedCount<T>> flipControl, Clock clock) {
        super(active.getMetricClazz());
        this.flipControl = flipControl;
        this.clock = clock;
        this.flipControl.flip(active);
        this.inactive = inactive;
        this.intervalStart = clock.nanoTime();
    }

    @Override
    public void write(T result, long number, long nanoTime) {
        long permit = flipControl.startRecord();
        try {
            flipControl.active().add(result, number);
        } finally {
            flipControl.endRecord(permit);
        }
    }

    @Override
    public PartitionedCount<T> activeInterval() {
        return flipControl.active();
    }

    @Override
    public long activeIntervalStart() {
        synchronized (lock) {
            return intervalStart;
        }
    }

    @Override
    public PartitionedCount<T> captureInterval() {
        return captureInterval(clock.nanoTime());
    }

    @Override
    public PartitionedCount<T> captureInterval(long nanotime) {
        inactive.reset();
        return captureInterval(inactive);
    }

    @Override
    public PartitionedCount<T> captureInterval(PartitionedCount<T> newInterval) {
        return captureInterval(newInterval, clock.nanoTime());
    }

    @Override
    public PartitionedCount<T> captureInterval(PartitionedCount<T> newInterval, long nanoTime) {
        synchronized (lock) {
            PartitionedCount<T> newlyInactive = flipControl.flip(newInterval);
            inactive = newlyInactive;
            intervalStart = nanoTime;
            return newlyInactive;
        }
    }

    public static <T extends Enum<T>> CountRecorderBuilder<T> builder(Class<T> clazz) {
        return new CountRecorderBuilder<>(clazz);
    }
}
