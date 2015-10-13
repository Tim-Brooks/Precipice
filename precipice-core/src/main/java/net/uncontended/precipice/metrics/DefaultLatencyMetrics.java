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

import java.util.concurrent.TimeUnit;

public class DefaultLatencyMetrics implements LatencyMetrics {

    private final Histogram histogram;

    public DefaultLatencyMetrics() {
        this(new AtomicHistogram(TimeUnit.HOURS.toNanos(1), 2));
    }

    public DefaultLatencyMetrics(Histogram histogram) {
        this.histogram = histogram;
    }

    @Override
    public void recordLatency(long nanoLatency) {
        recordLatency(nanoLatency, System.nanoTime());
    }

    @Override
    public void recordLatency(long nanoLatency, long nanoTime) {
        if (nanoLatency != -1) {
            histogram.recordValue(Math.min(nanoLatency, histogram.getHighestTrackableValue()));
        }
    }

    public LatencyBucket getLatencySnapshot() {
        // Getting values from histogram is not threadsafe. Need to evaluate the best way to do snapshot.
        LatencyBucket latencyBucket = new LatencyBucket();

        latencyBucket.latency50 = histogram.getValueAtPercentile(50.0);
        latencyBucket.latency90 = histogram.getValueAtPercentile(90.0);
        latencyBucket.latency99 = histogram.getValueAtPercentile(99.0);
        latencyBucket.latency999 = histogram.getValueAtPercentile(99.9);
        latencyBucket.latency9999 = histogram.getValueAtPercentile(99.99);
        latencyBucket.latency99999 = histogram.getValueAtPercentile(99.999);
        latencyBucket.latencyMax = histogram.getMaxValue();
        latencyBucket.latencyMean = histogram.getMean();
        return latencyBucket;
    }
}
