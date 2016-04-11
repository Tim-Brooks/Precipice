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

public class VariableIntervalLatency<T extends Enum<T>> extends AbstractMetrics<T> implements WritableLatencyMetrics<T> {

    private final Recorder<WritableLatencyMetrics<T>> recorder;
    private final Clock clock;
    private final LatencyFactory latencyFactory;

    public VariableIntervalLatency(Class<T> clazz) {
        this(clazz, Latency.atomicHDRHistogram());
    }

    public VariableIntervalLatency(Class<T> clazz, LatencyFactory latencyFactory) {
        this(clazz, latencyFactory, new RelaxedRecorder<>(latencyFactory.newLatency(clazz), System.nanoTime()));
    }

    public VariableIntervalLatency(Class<T> clazz, LatencyFactory latencyFactory, Clock clock) {
        this(clazz, latencyFactory, new RelaxedRecorder<>(latencyFactory.newLatency(clazz), clock.nanoTime()), clock);
    }

    public VariableIntervalLatency(Class<T> clazz, LatencyFactory latencyFactory, Recorder<WritableLatencyMetrics<T>> recorder) {
        this(clazz, latencyFactory, recorder, new SystemTime());
    }

    public VariableIntervalLatency(Class<T> clazz, LatencyFactory latencyFactory, Recorder<WritableLatencyMetrics<T>> recorder, Clock clock) {
        super(clazz);
        this.latencyFactory = latencyFactory;
        this.recorder = recorder;
        this.clock = clock;
    }

    @Override
    public void record(T result, long number, long nanoLatency) {
        record(result, number, nanoLatency, clock.nanoTime());
    }

    @Override
    public void record(T result, long number, long nanoLatency, long nanoTime) {
        recorder.active().record(result, number, nanoLatency, nanoTime);
    }

    public synchronized WritableLatencyMetrics<T> flip() {
        return recorder.flip(clock.nanoTime(), latencyFactory.newLatency(clazz));
    }

}
