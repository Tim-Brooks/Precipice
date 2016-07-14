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
import net.uncontended.precipice.metrics.tools.Allocator;
import net.uncontended.precipice.metrics.tools.Recorder;
import net.uncontended.precipice.metrics.tools.RelaxedRecorder;

public class CountRecorder<T extends Enum<T>> extends AbstractMetrics<T> implements WritableCounts<T> {

    private final Allocator<? extends PartitionedCount<T>> allocator;
    private final Recorder<PartitionedCount<T>> recorder;

    public CountRecorder(Class<T> clazz) {
        this(clazz, Counters.longAdder(clazz), new RelaxedRecorder<PartitionedCount<T>>());
    }

    public CountRecorder(Class<T> clazz, Allocator<? extends PartitionedCount<T>> allocator) {
        this(clazz, allocator, new RelaxedRecorder<PartitionedCount<T>>());
    }

    public CountRecorder(Class<T> clazz, Allocator<? extends PartitionedCount<T>> allocator, Recorder<PartitionedCount<T>> recorder) {
        super(clazz);
        this.allocator = allocator;
        this.recorder = recorder;
        this.recorder.flip(allocator.allocateNew());
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

    public PartitionedCount<T> capture() {
        return recorder.flip(allocator.allocateNew());
    }

    public PartitionedCount<T> capture(PartitionedCount<T> newCounter) {
        return recorder.flip(newCounter);
    }
}
