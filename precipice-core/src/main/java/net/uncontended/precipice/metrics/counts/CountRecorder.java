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
import net.uncontended.precipice.metrics.tools.MetricRecorder;
import net.uncontended.precipice.metrics.tools.Recorder;

public class CountRecorder<T extends Enum<T>> extends AbstractMetrics<T> implements WritableCounts<T>, Recorder<PartitionedCount<T>> {

    private final MetricRecorder<PartitionedCount<T>> metricRecorder;

    public CountRecorder(MetricRecorder<PartitionedCount<T>> metricRecorder) {
        super(metricRecorder.activeInterval().getMetricClazz());
        this.metricRecorder = metricRecorder;
    }

    @Override
    public void write(T result, long number, long nanoTime) {
        long permit = metricRecorder.startRecord();
        try {
            metricRecorder.activeInterval().add(result, number);
        } finally {
            metricRecorder.endRecord(permit);
        }
    }

    @Override
    public PartitionedCount<T> activeInterval() {
        return metricRecorder.activeInterval();
    }

    @Override
    public long activeIntervalStart() {
        return metricRecorder.activeIntervalStart();
    }

    @Override
    public PartitionedCount<T> captureInterval() {
        return metricRecorder.captureInterval();
    }

    @Override
    public PartitionedCount<T> captureInterval(long nanotime) {
        return metricRecorder.captureInterval(nanotime);
    }

    @Override
    public PartitionedCount<T> captureInterval(PartitionedCount<T> newInterval) {
        return metricRecorder.captureInterval(newInterval);
    }

    @Override
    public synchronized PartitionedCount<T> captureInterval(PartitionedCount<T> newInterval, long nanoTime) {
        return metricRecorder.captureInterval(newInterval, nanoTime);
    }

    public static <T extends Enum<T>> CountRecorderBuilder<T> builder(Class<T> clazz) {
        return new CountRecorderBuilder<>(clazz);
    }
}
