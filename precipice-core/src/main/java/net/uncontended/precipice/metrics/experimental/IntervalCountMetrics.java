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

package net.uncontended.precipice.metrics.experimental;

import net.uncontended.precipice.metrics.AddCounter;
import net.uncontended.precipice.metrics.CountMetrics;
import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;

public class IntervalCountMetrics<T extends Enum<T>> implements CountMetrics<T> {

    private long meta;
    private final Class<T> type;
    private final Clock clock;
    private final Holder<T> totalCounter;
    private volatile Holder<T> intervalCounter;
    private volatile Holder<T> lastInterval;

    public IntervalCountMetrics(Class<T> type) {
        this(type, new SystemTime());
    }

    public IntervalCountMetrics(Class<T> type, Clock clock) {
        this.type = type;
        this.clock = clock;
        this.totalCounter = new Holder<>(new AddCounter<>(this.type), meta++);
        this.intervalCounter = new Holder<>(new AddCounter<>(this.type), meta++);
    }

    @Override
    public void add(T metric, long delta) {
        add(metric, delta, clock.nanoTime());

    }

    @Override
    public void add(T metric, long delta, long nanoTime) {
        intervalCounter.counter.add(metric, delta, nanoTime);
    }

    @Override
    public long getCount(T metric) {
        for (; ; ) {
            Holder<T> localTotal = this.totalCounter;
            Holder<T> localInterval = this.intervalCounter;
            if (localTotal.id != localInterval.id) {
                return localTotal.counter.getCount(metric) + localInterval.counter.getCount(metric);
            }
        }
    }

    @Override
    public Class<T> getMetricType() {
        return type;
    }

    public synchronized CountMetrics<T> intervalCounts() {
        return intervalCounts(clock.nanoTime());
    }

    public synchronized CountMetrics<T> intervalCounts(long nanoTime) {
        Holder<T> old = this.intervalCounter;
        this.intervalCounter = new Holder<>(new AddCounter<>(this.type), meta++);
        for (T metric : type.getEnumConstants()) {
            totalCounter.counter.add(metric, old.counter.getCount(metric));
        }
        this.lastInterval = old;
        return old.counter;
    }

    public CountMetrics<T> lastIntervalCounts() {
        return lastInterval.counter;
    }

    private static class Holder<T extends Enum<T>> {
        private CountMetrics<T> counter;
        private long id;

        private Holder(CountMetrics<T> counter, long id) {
            this.counter = counter;
            this.id = id;
        }
    }
}
