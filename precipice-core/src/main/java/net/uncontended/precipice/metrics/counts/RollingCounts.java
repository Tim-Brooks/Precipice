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
import net.uncontended.precipice.metrics.IntervalIterator;
import net.uncontended.precipice.metrics.Rolling;
import net.uncontended.precipice.metrics.tools.CircularBuffer;
import net.uncontended.precipice.metrics.tools.RollingMetrics;
import net.uncontended.precipice.time.SystemTime;

public class RollingCounts<T extends Enum<T>> extends AbstractMetrics<T> implements WritableCounts<T>, Rolling<PartitionedCount<T>> {

    private final NoOpCounter<T> noOpCounter;

    private final RollingMetrics<PartitionedCount<T>> rolling;

    public RollingCounts(Class<T> clazz, int buckets, long nanosPerBucket) {
        this(new RollingMetrics<PartitionedCount<T>>(Counters.longAdder(clazz),
                new CircularBuffer<PartitionedCount<T>>(buckets, nanosPerBucket, System.nanoTime()),
                SystemTime.getInstance()));
    }

    public RollingCounts(RollingMetrics<PartitionedCount<T>> rolling) {
        super(rolling.current().getMetricClazz());
        this.rolling = rolling;
        this.noOpCounter = new NoOpCounter<>(getMetricClazz());
    }

    @Override
    public void write(T metric, long number, long nanoTime) {
        current(nanoTime).add(metric, number);
    }

    @Override
    public PartitionedCount<T> current() {
        return rolling.current();
    }

    @Override
    public PartitionedCount<T> current(long nanoTime) {
        return rolling.current(nanoTime);
    }

    @Override
    public IntervalIterator<PartitionedCount<T>> intervals() {
        return rolling.intervalsWithDefault(noOpCounter);
    }

    @Override
    public IntervalIterator<PartitionedCount<T>> intervals(long nanoTime) {
        return rolling.intervalsWithDefault(nanoTime, noOpCounter);
    }

    public static <V extends Enum<V>> RollingCountsBuilder<V> builder(Class<V> clazz) {
        return new RollingCountsBuilder<>(clazz);

    }
}
