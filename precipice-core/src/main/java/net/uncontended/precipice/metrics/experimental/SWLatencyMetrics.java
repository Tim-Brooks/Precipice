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

package net.uncontended.precipice.metrics.experimental;

import net.uncontended.precipice.Failable;
import net.uncontended.precipice.metrics.BackgroundTask;
import net.uncontended.precipice.metrics.CircularBuffer;
import net.uncontended.precipice.metrics.IntervalLatencyMetrics;
import net.uncontended.precipice.metrics.LatencySnapshot;
import org.HdrHistogram.AtomicHistogram;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

/**
 * Unstable and still in development. At this time, {@link IntervalLatencyMetrics} should be used.
 */
public class SWLatencyMetrics<T extends Enum<T> & Failable> implements BackgroundTask {

    private final LatencyBucket[] buckets;

    public SWLatencyMetrics(Class<T> type, long highestTrackableValue, int numberOfSignificantValueDigits) {
        T[] metricValues = type.getEnumConstants();

        buckets = new LatencyBucket[metricValues.length];
        for (T metric : metricValues) {
            buckets[metric.ordinal()] = new LatencyBucket(highestTrackableValue, numberOfSignificantValueDigits);
        }
    }

    public void recordLatency(T metric, long nanoLatency) {
        getLatencyBucket(metric).record(nanoLatency);

    }

    public void recordLatency(T metric, long nanoLatency, long nanoTime) {
        getLatencyBucket(metric).record(nanoLatency);
    }

    public LatencySnapshot latencySnapshot() {
        return null;
    }

    public LatencySnapshot latencySnapshot(T metric) {
        return null;
    }

    @Override
    public void tick(long nanoTime) {

    }

    private LatencyBucket getLatencyBucket(T metric) {
        return buckets[metric.ordinal()];
    }

    private static class LatencyBucket {
        private final Histogram histogram;
        private final Recorder recorder;
        private Histogram inactive;

        private LatencyBucket(long highestTrackableValue, int numberOfSignificantValueDigits) {
            histogram = new AtomicHistogram(highestTrackableValue, numberOfSignificantValueDigits);
            histogram.setStartTimeStamp(System.currentTimeMillis());

            recorder = new Recorder(highestTrackableValue, numberOfSignificantValueDigits);
            inactive = recorder.getIntervalHistogram();
        }

        private void record(long nanoLatency) {
            recorder.recordValue(Math.min(nanoLatency, histogram.getHighestTrackableValue()));
            histogram.recordValue(Math.min(nanoLatency, histogram.getHighestTrackableValue()));
        }

        private Histogram getIntervalHistogram() {
            Histogram intervalHistogram = recorder.getIntervalHistogram(inactive);
            inactive = intervalHistogram;
            return intervalHistogram;
        }
    }
}