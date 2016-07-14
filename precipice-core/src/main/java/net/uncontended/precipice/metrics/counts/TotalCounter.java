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

package net.uncontended.precipice.metrics.counts;

import net.uncontended.precipice.metrics.AbstractMetrics;

public class TotalCounter<T extends Enum<T>> extends AbstractMetrics<T> implements WritableCounts<T>, PartitionedCount<T> {

    private final PartitionedCount<T> counter;

    public TotalCounter(Class<T> clazz) {
        super(clazz);
        counter = new LongAdderCounter<>(clazz);
    }

    public TotalCounter(PartitionedCount<T> counter) {
        super(counter.getMetricClazz());
        this.counter = counter;
    }

    @Override
    public void write(T result, long number, long nanoTime) {
        counter.add(result, number);
    }

    @Override
    public long getCount(T metric) {
        return counter.getCount(metric);
    }

    @Override
    public long total() {
        return counter.total();
    }

    @Override
    public void add(T metric, long delta) {
        counter.add(metric, delta);

    }

    @Override
    public void reset() {
        counter.reset();
    }
}
