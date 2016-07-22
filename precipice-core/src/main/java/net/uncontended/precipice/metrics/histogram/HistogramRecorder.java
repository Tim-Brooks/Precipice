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

package net.uncontended.precipice.metrics.histogram;

import net.uncontended.precipice.metrics.AbstractMetrics;
import net.uncontended.precipice.metrics.tools.Recorder;
import net.uncontended.precipice.metrics.tools.RelaxedRecorder;

public class HistogramRecorder<T extends Enum<T>> extends AbstractMetrics<T> implements WritableLatency<T> {

    private final Recorder<PartitionedHistogram<T>> recorder;
    private PartitionedHistogram<T> inactive;

    public HistogramRecorder(PartitionedHistogram<T> active, PartitionedHistogram<T> inactive) {
        this(active, inactive, new RelaxedRecorder<PartitionedHistogram<T>>());
    }

    public HistogramRecorder(PartitionedHistogram<T> active, PartitionedHistogram<T> inactive, Recorder<PartitionedHistogram<T>> recorder) {
        super(active.getMetricClazz());
        this.recorder = recorder;
        this.recorder.flip(active);
        this.inactive = inactive;
    }

    @Override
    public void write(T result, long number, long nanoLatency, long nanoTime) {
        long permit = recorder.startRecord();
        try {
            recorder.active().record(result, number, nanoLatency);
        } finally {
            recorder.endRecord(permit);
        }
    }

    public synchronized PartitionedHistogram<T> captureInterval() {
        PartitionedHistogram<T> newlyInactive = recorder.flip(inactive);
        inactive = newlyInactive;
        return newlyInactive;
    }

    public synchronized PartitionedHistogram<T> captureInterval(PartitionedHistogram<T> newCounter) {
        PartitionedHistogram<T> newlyInactive = recorder.flip(newCounter);
        inactive = newlyInactive;
        return newlyInactive;
    }
}
