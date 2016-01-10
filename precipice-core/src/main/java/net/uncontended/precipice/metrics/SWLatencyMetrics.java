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

package net.uncontended.precipice.metrics;

import org.HdrHistogram.AtomicHistogram;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;

public class SWLatencyMetrics implements LatencyMetrics, BackgroundTask {

    private final LatencyBucket successBucket;
    private final LatencyBucket errorBucket;
    private final LatencyBucket timeoutBucket;

    public SWLatencyMetrics(long highestTrackableValue, int numberOfSignificantValueDigits) {
        successBucket = new LatencyBucket(highestTrackableValue, numberOfSignificantValueDigits);
        errorBucket = new LatencyBucket(highestTrackableValue, numberOfSignificantValueDigits);
        timeoutBucket = new LatencyBucket(highestTrackableValue, numberOfSignificantValueDigits);
    }

    @Override
    public void recordLatency(Metric metric, long nanoLatency) {

    }

    @Override
    public void recordLatency(Metric metric, long nanoLatency, long nanoTime) {

    }

    @Override
    public LatencySnapshot latencySnapshot() {
        return null;
    }

    @Override
    public LatencySnapshot latencySnapshot(Metric metric) {
        return null;
    }

    @Override
    public void tick(long nanoTime) {

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
