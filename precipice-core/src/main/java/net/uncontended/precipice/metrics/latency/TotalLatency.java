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

public class TotalLatency<T extends Enum<T>> extends AbstractMetrics<T> implements WritableLatency<T>, PartitionedLatency<T> {

    private final PartitionedLatency<T> latency;

    public TotalLatency(PartitionedLatency<T> latency) {
        super(latency.getMetricClazz());
        this.latency = latency;
    }

    public TotalLatency(Class<T> clazz) {
        super(clazz);
        latency = new ConcurrentHistogram<>(clazz);
    }

    @Override
    public void record(T result, long number, long nanoLatency) {
        latency.record(result, number, nanoLatency);
    }

    @Override
    public Histogram getHistogram(T metric) {
        return latency.getHistogram(metric);
    }

    @Override
    public long getValueAtPercentile(T metric, double percentile) {
        return latency.getValueAtPercentile(metric, percentile);
    }

    @Override
    public boolean isHDR() {
        return latency.isHDR();
    }

    @Override
    public void reset() {
        latency.reset();
    }

    @Override
    public void write(T metric, long number, long nanoLatency, long nanoTime) {
        record(metric, number, nanoLatency);
    }
}
