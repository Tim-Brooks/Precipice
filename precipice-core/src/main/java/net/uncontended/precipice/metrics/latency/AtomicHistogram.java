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

import java.util.concurrent.TimeUnit;

public class AtomicHistogram<T extends Enum<T>> extends AbstractMetrics<T> implements PartitionedHistogram<T> {

    private final Histogram[] histograms;

    public AtomicHistogram(Class<T> clazz) {
        this(clazz, TimeUnit.HOURS.toNanos(1), 2);
    }

    public AtomicHistogram(Class<T> clazz, long highestTrackableValue, int numberOfSignificantValueDigits) {
        super(clazz);
        T[] enumConstants = clazz.getEnumConstants();
        histograms = new Histogram[enumConstants.length];
        for (int i = 0; i < enumConstants.length; ++i) {
            histograms[i] = new org.HdrHistogram.AtomicHistogram(highestTrackableValue, numberOfSignificantValueDigits);
        }
    }

    @Override
    public void record(T metric, long number, long nanoLatency) {
        Histogram histogram = histograms[metric.ordinal()];
        histogram.recordValueWithCount(nanoLatency, number);
    }

    @Override
    public Histogram getHistogram(T metric) {
        return histograms[metric.ordinal()];
    }

    @Override
    public void reset() {
        for (Histogram histogram : histograms) {
            histogram.reset();
        }
    }
}
