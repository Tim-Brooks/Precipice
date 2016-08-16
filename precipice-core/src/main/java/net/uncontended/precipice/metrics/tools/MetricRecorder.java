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

package net.uncontended.precipice.metrics.tools;

import net.uncontended.precipice.metrics.Resettable;
import net.uncontended.precipice.time.Clock;

public class MetricRecorder<T extends Resettable> implements Recorder<T> {

    private final FlipControl<T> flipControl;
    private final Clock clock;
    private T inactive;
    private volatile long intervalStart;

    public MetricRecorder(T active, T inactive, FlipControl<T> flipControl, Clock clock) {
        this.flipControl = flipControl;
        this.clock = clock;
        this.flipControl.flip(active);
        this.inactive = inactive;
        this.intervalStart = clock.nanoTime();
    }

    @Override
    public T activeInterval() {
        return flipControl.active();
    }

    @Override
    public long activeIntervalStart() {
        return intervalStart;
    }

    @Override
    public T captureInterval() {
        return captureInterval(clock.nanoTime());
    }

    @Override
    public T captureInterval(long nanotime) {
        inactive.reset();
        return captureInterval(inactive, nanotime);
    }

    @Override
    public T captureInterval(T newInterval) {
        return captureInterval(newInterval, clock.nanoTime());
    }

    @Override
    public synchronized T captureInterval(T newInterval, long nanoTime) {
        T newlyInactive = flipControl.flip(newInterval);
        inactive = newlyInactive;
        intervalStart = nanoTime;
        return newlyInactive;
    }

    public long startRecord() {
        return flipControl.startRecord();
    }

    public void endRecord(long permit) {
        flipControl.endRecord(permit);
    }
}
