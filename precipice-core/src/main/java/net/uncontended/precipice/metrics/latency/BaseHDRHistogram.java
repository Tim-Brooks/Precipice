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

package net.uncontended.precipice.metrics.latency;

import net.uncontended.precipice.metrics.AbstractMetrics;
import org.HdrHistogram.Histogram;

public class BaseHDRHistogram<T extends Enum<T>> extends AbstractMetrics<T> implements PartitionedLatency<T> {
    protected final Histogram[] histograms;

    public BaseHDRHistogram(Class<T> clazz, Histogram[] histograms) {
        super(clazz);
        this.histograms = histograms;
    }

    @Override
    public void record(T metric, long number, long nanoLatency) {
        Histogram histogram = histograms[metric.ordinal()];
        long highestTrackableValue = histogram.getHighestTrackableValue();
        long potentiallyTruncated = highestTrackableValue > nanoLatency ? nanoLatency : highestTrackableValue;
        histogram.recordValueWithCount(potentiallyTruncated, number);
    }

    @Override
    public Histogram getHistogram(T metric) {
        return histograms[metric.ordinal()];
    }

    @Override
    public long getValueAtPercentile(T metric, double percentile) {
        return histograms[metric.ordinal()].getValueAtPercentile(percentile);
    }

    @Override
    public boolean isHDR() {
        return true;
    }

    @Override
    public void reset() {
        for (Histogram histogram : histograms) {
            histogram.reset();
        }
    }
}
