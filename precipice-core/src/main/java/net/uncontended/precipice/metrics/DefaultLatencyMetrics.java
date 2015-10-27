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

import org.HdrHistogram.AtomicHistogram;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramIterationValue;
import org.HdrHistogram.RecordedValuesIterator;

import java.util.concurrent.TimeUnit;

public class DefaultLatencyMetrics implements LatencyMetrics {

    private final Histogram successHistogram;
    private final Histogram errorHistogram;
    private final Histogram timeoutHistogram;

    public DefaultLatencyMetrics() {
        this(TimeUnit.HOURS.toNanos(1), 2);
    }

    public DefaultLatencyMetrics(long highestTrackableValue, int numberOfSignificantValueDigits) {
        successHistogram = new AtomicHistogram(highestTrackableValue, numberOfSignificantValueDigits);
        errorHistogram = new AtomicHistogram(highestTrackableValue, numberOfSignificantValueDigits);
        timeoutHistogram = new AtomicHistogram(highestTrackableValue, numberOfSignificantValueDigits);
    }

    @Override
    public void recordLatency(Metric metric, long nanoLatency) {
        recordLatency(metric, nanoLatency, System.nanoTime());
    }

    @Override
    public void recordLatency(Metric metric, long nanoLatency, long nanoTime) {
        if (nanoLatency != -1) {
            Histogram histogram;
            switch (metric) {
                case SUCCESS:
                    histogram = successHistogram;
                    break;
                case ERROR:
                    histogram = errorHistogram;
                    break;
                case TIMEOUT:
                    histogram = timeoutHistogram;
                    break;
                default:
                    throw new IllegalArgumentException("No latency capture for: " + metric);
            }
            histogram.recordValue(Math.min(nanoLatency, histogram.getHighestTrackableValue()));
        }
    }

    @Override
    public LatencySnapshot latencySnapshot(Metric metric) {
        Histogram histogram;
        switch (metric) {
            case SUCCESS:
                histogram = successHistogram;
                break;
            case ERROR:
                histogram = errorHistogram;
                break;
            case TIMEOUT:
                histogram = timeoutHistogram;
                break;
            default:
                throw new IllegalArgumentException("No latency capture for: " + metric);
        }
        return createSnapshot(histogram);
    }

    @Override
    public LatencySnapshot latencySnapshot() {
        Histogram accumulated = new Histogram(successHistogram);
        accumulated.add(successHistogram);
        accumulated.add(errorHistogram);
        accumulated.add(timeoutHistogram);

        return createSnapshot(accumulated);
    }

    private LatencySnapshot createSnapshot(Histogram histogram) {
        LatencySnapshot latencySnapshot = new LatencySnapshot();
        latencySnapshot.latency50 = histogram.getValueAtPercentile(50.0);
        latencySnapshot.latency90 = histogram.getValueAtPercentile(90.0);
        latencySnapshot.latency99 = histogram.getValueAtPercentile(99.0);
        latencySnapshot.latency999 = histogram.getValueAtPercentile(99.9);
        latencySnapshot.latency9999 = histogram.getValueAtPercentile(99.99);
        latencySnapshot.latency99999 = histogram.getValueAtPercentile(99.999);
        latencySnapshot.latencyMax = histogram.getMaxValue();
        latencySnapshot.latencyMean = calculateMean(histogram);
        return latencySnapshot;
    }

    private double calculateMean(Histogram histogram) {
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
}
