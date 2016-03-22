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

import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;

public class RelaxedCountInterval<T extends Enum<T>> implements CountMetrics<T>, Interval<CountMetrics<T>> {

    private final Class<T> clazz;
    private final CounterFactory intervalFactory;
    private final Clock clock;
    private final CountMetrics<T> totalCounter;
    private volatile CountMetrics<T> intervalCounter;

    public RelaxedCountInterval(Class<T> clazz) {
        this(clazz, Counters.adding());
    }

    public RelaxedCountInterval(Class<T> clazz, CounterFactory intervalFactory) {
        this(clazz, intervalFactory, new NoOpCounter<>(clazz));
    }

    public RelaxedCountInterval(Class<T> clazz, CounterFactory intervalFactory, CountMetrics<T> totalCounter) {
        this(clazz, intervalFactory, totalCounter, new SystemTime());
    }

    public RelaxedCountInterval(Class<T> clazz, CounterFactory intervalFactory, CountMetrics<T> totalCounter,
                                Clock clock) {
        this.clazz = clazz;
        this.intervalFactory = intervalFactory;
        this.clock = clock;
        long nanoTime = clock.nanoTime();
        this.totalCounter = totalCounter;
        this.intervalCounter = intervalFactory.newCounter(clazz, nanoTime);
    }

    @Override
    public void add(T metric, long delta) {
        add(metric, delta, clock.nanoTime());

    }

    @Override
    public void add(T metric, long delta, long nanoTime) {
        totalCounter.add(metric, delta, nanoTime);
        intervalCounter.add(metric, delta, nanoTime);
    }

    @Override
    public long getCount(T metric) {
        return this.totalCounter.getCount(metric);
    }

    @Override
    public Class<T> getMetricType() {
        return clazz;
    }

    @Override
    public CountMetrics<T> current() {
        return intervalCounter;
    }

    public synchronized CountMetrics<T> interval() {
        return interval(clock.nanoTime());
    }

    public synchronized CountMetrics<T> interval(long nanoTime) {
        return interval(nanoTime, intervalFactory.newCounter(clazz, nanoTime));
    }

    @Override
    public synchronized CountMetrics<T> interval(long nanoTime, CountMetrics<T> newVersion) {
        CountMetrics<T> old = this.intervalCounter;
        this.intervalCounter = newVersion;
        return old;
    }
}
