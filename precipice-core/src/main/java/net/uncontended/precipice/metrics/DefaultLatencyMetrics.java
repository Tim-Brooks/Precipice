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
        this(new AtomicHistogram(TimeUnit.HOURS.toNanos(1), 2));
    }

    public DefaultLatencyMetrics(Histogram histogram) {
        this.successHistogram = histogram;
        this.errorHistogram = new AtomicHistogram(TimeUnit.HOURS.toNanos(1), 2);
        this.timeoutHistogram = new AtomicHistogram(TimeUnit.HOURS.toNanos(1), 2);
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
                    histogram = this.successHistogram;
                    break;
                case ERROR:
                    histogram = this.errorHistogram;
                    break;
                case TIMEOUT:
                    histogram = this.timeoutHistogram;
                    break;
                default:
                    throw new IllegalArgumentException("No latency capture for: " + metric);
            }
            histogram.recordValue(Math.min(nanoLatency, histogram.getHighestTrackableValue()));
        }
    }

    public LatencyBucket getLatencySnapshot() {
        // Getting values from histogram is not threadsafe. Need to evaluate the best way to do snapshot.
        LatencyBucket latencyBucket = new LatencyBucket();

        // Need to accumulate histograms.
        latencyBucket.latency50 = successHistogram.getValueAtPercentile(50.0);
        latencyBucket.latency90 = successHistogram.getValueAtPercentile(90.0);
        latencyBucket.latency99 = successHistogram.getValueAtPercentile(99.0);
        latencyBucket.latency999 = successHistogram.getValueAtPercentile(99.9);
        latencyBucket.latency9999 = successHistogram.getValueAtPercentile(99.99);
        latencyBucket.latency99999 = successHistogram.getValueAtPercentile(99.999);
        latencyBucket.latencyMax = successHistogram.getMaxValue();
        latencyBucket.latencyMean = calculateMean();
        return latencyBucket;
    }

    private double calculateMean() {
        if (successHistogram.getTotalCount() == 0) {
            return 0.0;
        }
        RecordedValuesIterator iter = new RecordedValuesIterator(successHistogram);
        double totalValue = 0;
        while (iter.hasNext()) {
            HistogramIterationValue iterationValue = iter.next();
            totalValue += successHistogram.medianEquivalentValue(iterationValue.getValueIteratedTo())
                    * iterationValue.getCountAtValueIteratedTo();
        }
        return (totalValue * 1.0) / successHistogram.getTotalCount();
    }
}
