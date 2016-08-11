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
import net.uncontended.precipice.metrics.tools.Capturer;
import net.uncontended.precipice.metrics.tools.Recorder;
import net.uncontended.precipice.metrics.tools.RelaxedRecorder;

public class CountRecorder<T extends Enum<T>> extends AbstractMetrics<T> implements WritableCounts<T>, Capturer<PartitionedCount<T>> {

    private final Recorder<PartitionedCount<T>> recorder;
    private PartitionedCount<T> inactive;

    public CountRecorder(PartitionedCount<T> active, PartitionedCount<T> inactive) {
        this(active, inactive, new RelaxedRecorder<PartitionedCount<T>>());
    }

    public CountRecorder(PartitionedCount<T> active, PartitionedCount<T> inactive, Recorder<PartitionedCount<T>> recorder) {
        super(active.getMetricClazz());
        this.recorder = recorder;
        this.recorder.flip(active);
        this.inactive = inactive;
    }

    @Override
    public void write(T result, long number, long nanoTime) {
        long permit = recorder.startRecord();
        try {
            recorder.active().add(result, number);
        } finally {
            recorder.endRecord(permit);
        }
    }

    @Override
    public synchronized PartitionedCount<T> captureInterval() {
        inactive.reset();
        PartitionedCount<T> newlyInactive = recorder.flip(inactive);
        inactive = newlyInactive;
        return newlyInactive;
    }

    @Override
    public synchronized PartitionedCount<T> captureInterval(PartitionedCount<T> newInterval) {
        PartitionedCount<T> newlyInactive = recorder.flip(newInterval);
        inactive = newlyInactive;
        return newlyInactive;
    }

    public static <T extends Enum<T>> CountRecorderBuilder<T> builder(Class<T> clazz) {
        return new CountRecorderBuilder<>(clazz);
    }
}
