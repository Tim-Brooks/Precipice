/*
 * Copyright 2015 Timothy Brooks
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


import org.HdrHistogram.*;

import java.util.concurrent.TimeUnit;

public class IntervalLatencyMetrics implements LatencyMetrics {

    private final LatencyBucket successBucket;
    private final LatencyBucket errorBucket;
    private final LatencyBucket timeoutBucket;

    public IntervalLatencyMetrics() {
        this(TimeUnit.HOURS.toNanos(1), 2);
    }

    public IntervalLatencyMetrics(long highestTrackableValue, int numberOfSignificantValueDigits) {
        successBucket = new LatencyBucket(highestTrackableValue, numberOfSignificantValueDigits);
        errorBucket = new LatencyBucket(highestTrackableValue, numberOfSignificantValueDigits);
        timeoutBucket = new LatencyBucket(highestTrackableValue, numberOfSignificantValueDigits);
    }

    @Override
    public void recordLatency(Metric metric, long nanoLatency) {
        recordLatency(metric, nanoLatency, System.nanoTime());
    }

    @Override
    public void recordLatency(Metric metric, long nanoLatency, long nanoTime) {
        if (nanoLatency != -1) {
            LatencyBucket bucket = getLatencyBucket(metric);
            bucket.record(nanoLatency);
        }
    }

    @Override
    public LatencySnapshot latencySnapshot(Metric metric) {
        LatencyBucket bucket = getLatencyBucket(metric);
        return createSnapshot(bucket.histogram, -1, -1);
    }

    @Override
    public LatencySnapshot latencySnapshot() {
        Histogram accumulated = new Histogram(successBucket.histogram);
        accumulated.add(successBucket.histogram);
        accumulated.add(errorBucket.histogram);
        accumulated.add(timeoutBucket.histogram);

        return createSnapshot(accumulated, -1, -1);
    }

    public synchronized LatencySnapshot intervalSnapshot(Metric metric) {
        LatencyBucket latencyBucket = getLatencyBucket(metric);
        Histogram histogram = latencyBucket.getIntervalHistogram();
        return createSnapshot(histogram, histogram.getStartTimeStamp(), histogram.getEndTimeStamp());
    }

    private LatencyBucket getLatencyBucket(Metric metric) {
        LatencyBucket bucket;
        switch (metric) {
            case SUCCESS:
                bucket = successBucket;
                break;
            case ERROR:
                bucket = errorBucket;
                break;
            case TIMEOUT:
                bucket = timeoutBucket;
                break;
            default:
                throw new IllegalArgumentException("No latency capture for: " + metric);
        }
        return bucket;
    }

    private static LatencySnapshot createSnapshot(Histogram histogram, long startTime, long endTime) {
        long latency50 = histogram.getValueAtPercentile(50.0);
        long latency90 = histogram.getValueAtPercentile(90.0);
        long latency99 = histogram.getValueAtPercentile(99.0);
        long latency999 = histogram.getValueAtPercentile(99.9);
        long latency9999 = histogram.getValueAtPercentile(99.99);
        long latency99999 = histogram.getValueAtPercentile(99.999);
        long latencyMax = histogram.getMaxValue();
        double latencyMean = calculateMean(histogram);
        return new LatencySnapshot(latency50, latency90, latency99, latency999, latency9999, latency99999, latencyMax,
                latencyMean, startTime, endTime);
    }

    private static double calculateMean(Histogram histogram) {
        if (histogram.getTotalCount() == 0) {
            return 0.0;
        }
        RecordedValuesIterator iter = new RecordedValuesIterator(histogram);
        double totalValue = 0;
        while (iter.hasNext()) {
            HistogramIterationValue iterationValue = iter.next();
            totalValue += histogram.medianEquivalentValue(iterationValue.getValueIteratedTo())
                    * iterationValue.getCountAtValueIteratedTo();
        }
        return totalValue * 1.0 / histogram.getTotalCount();
    }

    private static class LatencyBucket {
        private final Histogram histogram;
        private final Recorder recorder;
        private Histogram inactive;

        private LatencyBucket(long highestTrackableValue, int numberOfSignificantValueDigits) {

            histogram = new AtomicHistogram(highestTrackableValue, numberOfSignificantValueDigits);
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
