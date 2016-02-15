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

package net.uncontended.precipice.metrics.experimental;

import net.uncontended.precipice.Failable;
import net.uncontended.precipice.metrics.IntervalLatencyMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.metrics.LatencySnapshot;
import net.uncontended.precipice.metrics.util.RawCircularBuffer;
import org.HdrHistogram.*;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unstable and still in development. At this time, {@link IntervalLatencyMetrics} should be used.
 */
public class RollingLatencyMetrics<T extends Enum<T> & Failable> implements LatencyMetrics<T> {

    private final long startTime;

    private final LatencyBucket[] buckets;
    private final long highestTrackableValue;
    private final int numberOfSignificantValueDigits;

    public RollingLatencyMetrics(Class<T> type, long startTime) {
        this(type, startTime, TimeUnit.MINUTES.toNanos(10), TimeUnit.HOURS.toNanos(1), 2);
    }

    public RollingLatencyMetrics(Class<T> type, long startTime, long bucketResolution, long highestTrackableValue,
                                 int numberOfSignificantValueDigits) {
        this.startTime = startTime;
        this.highestTrackableValue = highestTrackableValue;
        this.numberOfSignificantValueDigits = numberOfSignificantValueDigits;
        buckets = new LatencyBucket[type.getEnumConstants().length];
        for (int i = 0; i < buckets.length; ++i) {
            buckets[i] = new LatencyBucket(startTime, bucketResolution, highestTrackableValue, numberOfSignificantValueDigits);
        }
    }

    @Override
    public void recordLatency(T metric, long nanoLatency) {
        recordLatency(metric, nanoLatency, System.nanoTime());
    }

    @Override
    public void recordLatency(T metric, long nanoLatency, long nanoTime) {
        if (nanoLatency != -1) {
            LatencyBucket bucket = getLatencyBucket(metric);
            bucket.record(nanoLatency, nanoTime);
        }
    }

    @Override
    public LatencySnapshot latencySnapshot(T metric) {
        LatencyBucket bucket = getLatencyBucket(metric);
        return createSnapshot(bucket.histogram, startTime, -1);
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

    public Iterable<LatencySnapshot> snapshotsForPeriod(T result, long timePeriod, TimeUnit timeUnit) {
        return snapshotsForPeriod(result, timePeriod, timeUnit, System.nanoTime());
    }

    public Iterable<LatencySnapshot> snapshotsForPeriod(T result, long timePeriod, TimeUnit timeUnit, long nanoTime) {
        LatencyBucket bucket = getLatencyBucket(result);
        return bucket.getIterable(timePeriod, timeUnit, nanoTime);
    }

    public Histogram getHistogram(T result) {
        LatencyBucket latencyBucket = getLatencyBucket(result);
        return latencyBucket.histogram;
    }

    public Histogram getInactiveHistogram(T result) {
        LatencyBucket latencyBucket = getLatencyBucket(result);
        // TODO: Threadsafe?
        return latencyBucket.inactive;
    }

    private LatencyBucket getLatencyBucket(T result) {
        return buckets[result.ordinal()];
    }

    private static LatencySnapshot createSnapshot(Histogram histogram, long startTime, long endTime) {
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
            return createSnapshot(intervalHistogram, startTime, endTime);
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

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("remove");
                }
            };
        }
    }
}
