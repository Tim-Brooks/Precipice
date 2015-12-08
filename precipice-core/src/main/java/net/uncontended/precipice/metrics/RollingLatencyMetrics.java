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

import net.uncontended.precipice.metrics.util.RawCircularBuffer;
import org.HdrHistogram.*;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unstable and still in development. At this time, {@link IntervalLatencyMetrics} should be used.
 */
public class RollingLatencyMetrics implements LatencyMetrics {

    public static final LatencySnapshot DEFAULT_SNAPSHOT = new LatencySnapshot(-1, -1, -1, -1, -1, -1, -1, -1.0, -1, -1);

    private final long startTime;

    private final LatencyBucket successBucket;
    private final LatencyBucket errorBucket;
    private final LatencyBucket timeoutBucket;

    public RollingLatencyMetrics(long startTime) {
        this(startTime, TimeUnit.MINUTES.toNanos(10), TimeUnit.HOURS.toNanos(1), 2);
    }

    public RollingLatencyMetrics(long startTime, long bucketResolution, long highestTrackableValue, int
            numberOfSignificantValueDigits) {
        this.startTime = startTime;
        successBucket = new LatencyBucket(startTime, bucketResolution, highestTrackableValue, numberOfSignificantValueDigits);
        errorBucket = new LatencyBucket(startTime, bucketResolution, highestTrackableValue, numberOfSignificantValueDigits);
        timeoutBucket = new LatencyBucket(startTime, bucketResolution, highestTrackableValue, numberOfSignificantValueDigits);
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
        return createSnapshot(startTime, -1, bucket.histogram);
    }

    @Override
    public LatencySnapshot latencySnapshot() {
        Histogram accumulated = new Histogram(successBucket.histogram);
        accumulated.add(successBucket.histogram);
        accumulated.add(errorBucket.histogram);
        accumulated.add(timeoutBucket.histogram);

        return createSnapshot(startTime, -1, accumulated);
    }

    public Iterable<LatencySnapshot> snapshotsForPeriod(Metric metric, long timePeriod, TimeUnit timeUnit) {
        return snapshotsForPeriod(metric, timePeriod, timeUnit, System.nanoTime());
    }

    public Iterable<LatencySnapshot> snapshotsForPeriod(Metric metric, long timePeriod, TimeUnit timeUnit, long nanoTime) {
        LatencyBucket bucket = getLatencyBucket(metric);
        return bucket.getIterable(timePeriod, timeUnit, nanoTime);
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

    private static LatencySnapshot createSnapshot(long startTime, long endTime, Histogram histogram) {
        if (histogram.getTotalCount() != 0) {
            long latency50 = histogram.getValueAtPercentile(50.0);
            long latency90 = histogram.getValueAtPercentile(90.0);
            long latency99 = histogram.getValueAtPercentile(99.0);
            long latency999 = histogram.getValueAtPercentile(99.9);
            long latency9999 = histogram.getValueAtPercentile(99.99);
            long latency99999 = histogram.getValueAtPercentile(99.999);
            long latencyMax = histogram.getMaxValue();
            double latencyMean = calculateMean(histogram);
            return new LatencySnapshot(latency50, latency90, latency99, latency999, latency9999, latency99999,
                    latencyMax, latencyMean, histogram.getStartTimeStamp(), histogram.getEndTimeStamp());
        } else {
            return new LatencySnapshot(-1, -1, -1, -1, -1, -1, -1, -1.0, startTime, endTime);
        }
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
        private final RawCircularBuffer<LatencySnapshot> buffer;
        private final long bucketResolution;
        private final AtomicLong timeToSwitch;

        private long previousTime;
        private Histogram inactive;

        private LatencyBucket(long startTime, long bucketResolution, long highestTrackableValue, int numberOfSignificantValueDigits) {

            this.bucketResolution = bucketResolution;
            this.previousTime = startTime;
            this.buffer = new RawCircularBuffer<>(6, 10, TimeUnit.MINUTES, startTime);
            timeToSwitch = new AtomicLong(startTime + bucketResolution);
            histogram = new AtomicHistogram(highestTrackableValue, numberOfSignificantValueDigits);
            recorder = new Recorder(highestTrackableValue, numberOfSignificantValueDigits);
            inactive = recorder.getIntervalHistogram();
        }

        private void record(long nanoLatency, long nanoTime) {
            advanceBucketsIfNecessary(nanoTime);
            recorder.recordValue(Math.min(nanoLatency, histogram.getHighestTrackableValue()));
            histogram.recordValue(Math.min(nanoLatency, histogram.getHighestTrackableValue()));
        }

        private LatencySnapshot swapHistograms(long startTime, long endTime) {
            Histogram intervalHistogram = recorder.getIntervalHistogram(inactive);
            inactive = intervalHistogram;
            return createSnapshot(startTime, endTime, intervalHistogram);
        }

        private Iterable<LatencySnapshot> getIterable(long timePeriod, TimeUnit timeUnit, long nanoTime) {
            advanceBucketsIfNecessary(nanoTime);
            return new WrappingIterable(buffer.collectActiveSlotsForTimePeriod(timePeriod, timeUnit, nanoTime));
        }

        private void advanceBucketsIfNecessary(long nanoTime) {
            long timeToSwitch = this.timeToSwitch.get();

            long difference = nanoTime - timeToSwitch;
            if (difference > 1 && this.timeToSwitch.compareAndSet(timeToSwitch,
                    bucketResolution - difference % bucketResolution + nanoTime)) {
                buffer.put(previousTime, swapHistograms(previousTime, timeToSwitch));
                // TODO: Considering thread safety issues.
                previousTime = nanoTime;
            }
        }
    }

    private static class WrappingIterable implements Iterable<LatencySnapshot> {
        private final Iterable<RawCircularBuffer.Slot<LatencySnapshot>> iterable;

        private WrappingIterable(Iterable<RawCircularBuffer.Slot<LatencySnapshot>> iterable) {
            this.iterable = iterable;
        }

        @Override
        public Iterator<LatencySnapshot> iterator() {
            final Iterator<RawCircularBuffer.Slot<LatencySnapshot>> iterator = iterable.iterator();
            return new Iterator<LatencySnapshot>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public LatencySnapshot next() {
                    RawCircularBuffer.Slot<LatencySnapshot> next = iterator.next();
                    if (next != null) {
                        return next.object;
                    } else {
                        long startTime = next.absoluteSlot;
                        long endTime = startTime; // TODO + bucketResolution
                        return new LatencySnapshot(-1, -1, -1, -1, -1, -1, -1, -1.0, startTime, endTime);
                    }
                }
            };
        }
    }
}
