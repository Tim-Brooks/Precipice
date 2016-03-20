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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class IntervalCountMetrics<T extends Enum<T>> implements CountMetrics<T> {

    private final Class<T> type;
    private final Clock clock;
    private final CountMetrics<T> totalCounter;
    private final AtomicReference<CountMetrics<T>> intervalCounter;
    private volatile CountMetrics<T> previousInterval;

    public IntervalCountMetrics(Class<T> type) {
        this(type, new SystemTime());
    }

    public IntervalCountMetrics(Class<T> type, Clock clock) {
        this.type = type;
        this.clock = clock;
        this.totalCounter = new AddCounter<>(this.type);
        this.intervalCounter = new AtomicReference<CountMetrics<T>>(new AddCounter<>(this.type));
    }

    @Override
    public void add(T metric, long delta) {
        add(metric, delta, clock.nanoTime());

    }

    @Override
    public void add(T metric, long delta, long nanoTime) {
        totalCounter.add(metric, delta, nanoTime);
    }

    @Override
    public long getCount(T metric) {
        return totalCounter.getCount(metric);
    }

    @Override
    public Class<T> getMetricType() {
        return type;
    }

    public synchronized CountMetrics<T> intervalCounts() {
        return intervalCounter.getAndSet(new AddCounter<>(this.type));
    }

    public CountMetrics<T> previousIntervalCounts() {
        return previousInterval;
    }

    public Iterable<CountMetrics<T>> metricCounters(long timePeriod, TimeUnit timeUnit) {
        return metricCounters(timePeriod, timeUnit, clock.nanoTime());
    }

    public Iterable<CountMetrics<T>> metricCounters(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        return null;
    }
}
