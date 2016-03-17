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


import net.uncontended.precipice.Failable;
import org.HdrHistogram.*;

import java.util.concurrent.TimeUnit;

public class IntervalLatencyMetrics<T extends Enum<T> & Failable> implements LatencyMetrics<T> {

    private final LatencyBucket[] buckets;
    private final long highestTrackableValue;
    private final int numberOfSignificantValueDigits;

    public IntervalLatencyMetrics(Class<T> type) {
        this(type, TimeUnit.HOURS.toNanos(1), 2);
    }

    public IntervalLatencyMetrics(Class<T> type, long highestTrackableValue, int numberOfSignificantValueDigits) {
        this.highestTrackableValue = highestTrackableValue;
        this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
        buckets = new LatencyBucket[type.getEnumConstants().length];
        for (int i = 0; i < buckets.length; ++i) {
            buckets[i] = new LatencyBucket(highestTrackableValue, numberOfSignificantValueDigits);
        }
    }

    @Override
    public void recordLatency(T result, long number, long nanoLatency) {
        recordLatency(result, number, nanoLatency, System.nanoTime());
    }

    @Override
    public void recordLatency(T result, long count, long nanoLatency, long nanoTime) {
        LatencyBucket bucket = getLatencyBucket(result);
        bucket.record(nanoLatency, count);
    }

    @Override
    public LatencySnapshot latencySnapshot(T result) {
        LatencyBucket bucket = getLatencyBucket(result);
        return createSnapshot(bucket.histogram, bucket.histogram.getStartTimeStamp(), System.currentTimeMillis());

    }

    @Override
    public LatencySnapshot latencySnapshot() {
        Histogram accumulated = new Histogram(highestTrackableValue, numberOfSignificantValueDigits);

        long startTime = -1;
        for (LatencyBucket bucket : buckets) {
            if (bucket != null) {
                Histogram histogram = bucket.histogram;
                if (startTime == -1) {
                    startTime = histogram.getStartTimeStamp();
                } else {
                    startTime = Math.min(startTime, histogram.getStartTimeStamp());
                }
                accumulated.add(histogram);
            }
        }

        return createSnapshot(accumulated, startTime, System.currentTimeMillis());
    }

    public synchronized LatencySnapshot intervalSnapshot(T result) {
        LatencyBucket latencyBucket = getLatencyBucket(result);
        Histogram histogram = latencyBucket.getIntervalHistogram();
        return createSnapshot(histogram, histogram.getStartTimeStamp(), histogram.getEndTimeStamp());
    }

    private LatencyBucket getLatencyBucket(T result) {
        return buckets[result.ordinal()];
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
            histogram.setStartTimeStamp(System.currentTimeMillis());

            recorder = new Recorder(highestTrackableValue, numberOfSignificantValueDigits);
            inactive = recorder.getIntervalHistogram();
        }

        private void record(long nanoLatency, long count) {
            recorder.recordValueWithCount(Math.min(nanoLatency, histogram.getHighestTrackableValue()), count);
            histogram.recordValueWithCount(Math.min(nanoLatency, histogram.getHighestTrackableValue()), count);
        }

        private Histogram getIntervalHistogram() {
            Histogram intervalHistogram = recorder.getIntervalHistogram(inactive);
            inactive = intervalHistogram;
            return intervalHistogram;
        }
    }
}
