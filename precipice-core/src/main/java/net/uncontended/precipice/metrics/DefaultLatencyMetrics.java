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

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultLatencyMetrics implements LatencyMetrics {

    public static final LatencySnapshot DEFAULT_SNAPSHOT = new LatencySnapshot(-1, -1, -1, -1, -1, -1, -1, -1.0, -1, -1);

    private final LatencyBucket successBucket;
    private final LatencyBucket errorBucket;
    private final LatencyBucket timeoutBucket;

    public DefaultLatencyMetrics() {
        this(TimeUnit.MINUTES.toNanos(10), TimeUnit.HOURS.toNanos(1), 2);
    }

    public DefaultLatencyMetrics(long bucketResolution, long highestTrackableValue, int numberOfSignificantValueDigits) {
        successBucket = new LatencyBucket(bucketResolution, highestTrackableValue, numberOfSignificantValueDigits);
        errorBucket = new LatencyBucket(bucketResolution, highestTrackableValue, numberOfSignificantValueDigits);
        timeoutBucket = new LatencyBucket(bucketResolution, highestTrackableValue, numberOfSignificantValueDigits);
    }

    @Override
    public void recordLatency(Metric metric, long nanoLatency) {
        recordLatency(metric, nanoLatency, System.nanoTime());
    }

    @Override
    public void recordLatency(Metric metric, long nanoLatency, long nanoTime) {
        if (nanoLatency != -1) {
            LatencyBucket bucket = getLatencyBucket(metric);
            bucket.record(nanoLatency, nanoTime);
        }
    }

    @Override
    public LatencySnapshot latencySnapshot(Metric metric) {
        LatencyBucket bucket = getLatencyBucket(metric);
        return createSnapshot(bucket.histogram);
    }

    @Override
    public LatencySnapshot latencySnapshot() {
        Histogram accumulated = new Histogram(successBucket.histogram);
        accumulated.add(successBucket.histogram);
        accumulated.add(errorBucket.histogram);
        accumulated.add(timeoutBucket.histogram);

        return createSnapshot(accumulated);
    }

    public Iterable<LatencySnapshot> snapshotsForPeriod(Metric metric, long timePeriod, TimeUnit timeUnit) {
        return snapshotsForPeriod(metric, timePeriod, timeUnit, System.nanoTime());
    }

    public Iterable<LatencySnapshot> snapshotsForPeriod(Metric metric, long timePeriod, TimeUnit timeUnit, long nanoTime) {
        LatencyBucket bucket = getLatencyBucket(metric);
        CircularBuffer<LatencySnapshot> buffer = bucket.buffer;
        return new WrappingIterable(buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit, nanoTime));
    }

    public Histogram getHistogram(Metric metric) {
        LatencyBucket latencyBucket = getLatencyBucket(metric);
        return latencyBucket.histogram;
    }

    public Histogram getInactiveHistogram(Metric metric) {
        LatencyBucket latencyBucket = getLatencyBucket(metric);
        // TODO: Threadsafe?
        return latencyBucket.inactive;
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

    private static LatencySnapshot createSnapshot(Histogram histogram) {
        long latency50 = histogram.getValueAtPercentile(50.0);
        long latency90 = histogram.getValueAtPercentile(90.0);
        long latency99 = histogram.getValueAtPercentile(99.0);
        long latency999 = histogram.getValueAtPercentile(99.9);
        long latency9999 = histogram.getValueAtPercentile(99.99);
        long latency99999 = histogram.getValueAtPercentile(99.999);
        long latencyMax = histogram.getMaxValue();
        double latencyMean = calculateMean(histogram);
        return new LatencySnapshot(latency50, latency90, latency99, latency999, latency9999, latency99999, latencyMax,
                latencyMean, histogram.getStartTimeStamp(), histogram.getEndTimeStamp());
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
        final Histogram histogram;

        private final Recorder recorder;
        private final CircularBuffer<LatencySnapshot> buffer;
        private final long bucketResolution;
        private final AtomicLong timeToSwitch;

        private long previousTime;
        private Histogram inactive;

        private LatencyBucket(long bucketResolution, long highestTrackableValue, int numberOfSignificantValueDigits) {
            long currentTime = System.nanoTime();

            this.bucketResolution = bucketResolution;
            this.previousTime = currentTime;
            this.buffer = new CircularBuffer<>(6, 10, TimeUnit.MINUTES, currentTime);
            timeToSwitch = new AtomicLong(currentTime + bucketResolution);
            histogram = new AtomicHistogram(highestTrackableValue, numberOfSignificantValueDigits);
            recorder = new Recorder(highestTrackableValue, numberOfSignificantValueDigits);
            inactive = recorder.getIntervalHistogram();
        }

        public void record(long nanoLatency, long nanoTime) {
            long timeToSwitch = this.timeToSwitch.get();

            long difference = nanoTime - timeToSwitch;
            if (difference > 1 && this.timeToSwitch.compareAndSet(timeToSwitch,
                    bucketResolution - difference % bucketResolution + nanoTime)) {
                buffer.put(previousTime, swapHistograms());
                // TODO: Considering thread safety issues.
                previousTime = nanoTime;
            }
            recorder.recordValue(Math.min(nanoLatency, histogram.getHighestTrackableValue()));
            histogram.recordValue(Math.min(nanoLatency, histogram.getHighestTrackableValue()));
        }

        public LatencySnapshot swapHistograms() {
            Histogram intervalHistogram = recorder.getIntervalHistogram(inactive);
            inactive = intervalHistogram;
            return createSnapshot(intervalHistogram);
        }
    }

    private class WrappingIterable implements Iterable<LatencySnapshot> {
        private final Iterable<LatencySnapshot> iterable;

        private WrappingIterable(Iterable<LatencySnapshot> iterable) {
            this.iterable = iterable;
        }

        @Override
        public Iterator<LatencySnapshot> iterator() {
            final Iterator<LatencySnapshot> iterator = iterable.iterator();
            return new Iterator<LatencySnapshot>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public LatencySnapshot next() {
                    LatencySnapshot next = iterator.next();
                    if (next != null) {
                        return next;
                    } else {
                        return DEFAULT_SNAPSHOT;
                    }
                }
            };
        }
    }
}
