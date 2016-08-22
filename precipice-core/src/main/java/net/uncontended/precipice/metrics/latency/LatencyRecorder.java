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
import net.uncontended.precipice.metrics.tools.MetricRecorder;
import net.uncontended.precipice.metrics.tools.Recorder;

public class LatencyRecorder<T extends Enum<T>> extends AbstractMetrics<T> implements WritableLatency<T>, Recorder<PartitionedLatency<T>> {

    private final MetricRecorder<PartitionedLatency<T>> metricRecorder;

    public LatencyRecorder(MetricRecorder<PartitionedLatency<T>> metricRecorder) {
        super(metricRecorder.activeInterval().getMetricClazz());
        this.metricRecorder = metricRecorder;
    }

    @Override
    public void write(T metric, long number, long nanoLatency, long nanoTime) {
        long permit = metricRecorder.startRecord();
        try {
            metricRecorder.activeInterval().record(metric, number, nanoLatency);
        } finally {
            metricRecorder.endRecord(permit);
        }
    }

    @Override
    public PartitionedLatency<T> activeInterval() {
        return metricRecorder.activeInterval();
    }

    @Override
    public long activeIntervalStart() {
        return metricRecorder.activeIntervalStart();
    }

    @Override
    public PartitionedLatency<T> captureInterval() {
        return metricRecorder.captureInterval();
    }

    @Override
    public PartitionedLatency<T> captureInterval(long nanotime) {
        return metricRecorder.captureInterval(nanotime);
    }

    @Override
    public PartitionedLatency<T> captureInterval(PartitionedLatency<T> newInterval) {
        return metricRecorder.captureInterval(newInterval);
    }

    @Override
    public synchronized PartitionedLatency<T> captureInterval(PartitionedLatency<T> newInterval, long nanoTime) {
        return metricRecorder.captureInterval(newInterval, nanoTime);
    }

    public static <T extends Enum<T>> LatencyRecorderBuilder<T> builder(Class<T> clazz) {
        return new LatencyRecorderBuilder<>(clazz);
    }
}
