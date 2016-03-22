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

package net.uncontended.precipice.metrics;

import org.HdrHistogram.AtomicHistogram;
import org.HdrHistogram.Histogram;

import java.util.concurrent.TimeUnit;

public class AtomicHDRHistogram<T extends Enum<T>> implements LatencyMetrics<T> {

    private final Histogram[] histograms;
    private final Class<T> clazz;

    public AtomicHDRHistogram(Class<T> clazz) {
        this(clazz, TimeUnit.HOURS.toNanos(1), 2);
    }

    public AtomicHDRHistogram(Class<T> clazz, long highestTrackableValue, int numberOfSignificantValueDigits) {
        this.clazz = clazz;
        T[] enumConstants = clazz.getEnumConstants();
        histograms = new Histogram[enumConstants.length];
        for (int i = 0; i < enumConstants.length; ++i) {
            histograms[i] = new AtomicHistogram(highestTrackableValue, numberOfSignificantValueDigits);
        }
    }

    @Override
    public void recordLatency(T result, long number, long nanoLatency) {
        Histogram histogram = histograms[result.ordinal()];
        histogram.recordValueWithCount(nanoLatency, number);
    }

    @Override
    public void recordLatency(T result, long number, long nanoLatency, long nanoTime) {
        Histogram histogram = histograms[result.ordinal()];
        histogram.recordValueWithCount(nanoLatency, number);
    }

    public void reset() {
        for (Histogram histogram : histograms) {
            histogram.reset();
        }
    }

    public Class<T> getMetricType() {
        return clazz;
    }
}
