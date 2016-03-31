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

public class VariableIntervalCounts<T extends Enum<T>> extends AbstractMetrics<T> implements CountMetrics<T> {

    private final CounterFactory counterFactory;
    private final Clock clock;
    private Recorder<CountMetrics<T>> recorder;

    public VariableIntervalCounts(Class<T> clazz) {
        this(clazz, Counters.adding());
    }

    public VariableIntervalCounts(Class<T> clazz, CounterFactory counterFactory) {
        this(clazz, counterFactory, new RelaxedRecorder<>(counterFactory.newCounter(clazz), System.nanoTime()));
    }

    public VariableIntervalCounts(Class<T> clazz, CounterFactory counterFactory, Recorder<CountMetrics<T>> recorder) {
        this(clazz, counterFactory, recorder, new SystemTime());
    }

    public VariableIntervalCounts(Class<T> clazz, CounterFactory counterFactory, Clock clock) {
        this(clazz, counterFactory, new RelaxedRecorder<>(counterFactory.newCounter(clazz), clock.nanoTime()), clock);
    }

    public VariableIntervalCounts(Class<T> clazz, CounterFactory counterFactory,
                                  Recorder<CountMetrics<T>> recorder, Clock clock) {
        super(clazz);
        this.recorder = recorder;
        this.counterFactory = counterFactory;
        this.clock = clock;
    }

    @Override
    public void add(T metric, long delta) {
        add(metric, delta, clock.nanoTime());

    }

    @Override
    public void add(T metric, long delta, long nanoTime) {
        recorder.active().add(metric, delta, nanoTime);
    }

    @Override
    public long getCount(T metric) {
        return 0L;
    }

    public synchronized CountMetrics<T> flip() {
        return recorder.flip(clock.nanoTime(), counterFactory.newCounter(clazz));
    }

}
