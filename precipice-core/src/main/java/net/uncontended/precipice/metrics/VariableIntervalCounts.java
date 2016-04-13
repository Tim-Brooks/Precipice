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

public class VariableIntervalCounts<T extends Enum<T>> extends AbstractMetrics<T> implements WritableCounts<T> {

    private final Allocator counterFactory;
    private final Clock clock;
    private Recorder<PartitionedCount<T>> recorder;

    public VariableIntervalCounts(Class<T> clazz) {
        this(clazz, Counters.longAdder());
    }

    public VariableIntervalCounts(Class<T> clazz, Allocator counterFactory) {
        this(clazz, counterFactory, new RelaxedRecorder<>(counterFactory.allocateNew(clazz), System.nanoTime()));
    }

    public VariableIntervalCounts(Class<T> clazz, Allocator counterFactory,
                                  Recorder<PartitionedCount<T>> recorder) {
        this(clazz, counterFactory, recorder, new SystemTime());
    }

    public VariableIntervalCounts(Class<T> clazz, Allocator counterFactory, Clock clock) {
        this(clazz, counterFactory, new RelaxedRecorder<>(counterFactory.allocateNew(clazz), clock.nanoTime()), clock);
    }

    public VariableIntervalCounts(Class<T> clazz, Allocator counterFactory,
                                  Recorder<PartitionedCount<T>> recorder, Clock clock) {
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

    public synchronized PartitionedCount<T> flip() {
        return recorder.flip(clock.nanoTime(), counterFactory.allocateNew(clazz));
    }

}
