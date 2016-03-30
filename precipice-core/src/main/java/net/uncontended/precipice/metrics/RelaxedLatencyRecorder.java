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

public class RelaxedLatencyRecorder<T extends Enum<T>> extends RelaxedRecorder<T, LatencyMetrics<T>> implements LatencyMetrics<T>,
        Recorder<LatencyMetrics<T>> {

    private final Clock clock;
    private final LatencyFactory latencyFactory;

    public RelaxedLatencyRecorder(Class<T> clazz) {
        this(clazz, Latency.atomicHDRHistogram(), new SystemTime());
    }

    public RelaxedLatencyRecorder(Class<T> clazz, LatencyFactory latencyFactory) {
        this(clazz, latencyFactory, new SystemTime());
    }

    public RelaxedLatencyRecorder(Class<T> clazz, LatencyFactory latencyFactory, Clock clock) {
        super(clazz, latencyFactory.newLatency(clazz), clock.nanoTime());
        this.latencyFactory = latencyFactory;
        this.clock = clock;
    }

    @Override
    public void record(T result, long number, long nanoLatency) {
        record(result, number, nanoLatency, clock.nanoTime());
    }

    @Override
    public void record(T result, long number, long nanoLatency, long nanoTime) {
        active().record(result, number, nanoLatency, nanoTime);
    }

    @Override
    public PrecipiceHistogram getHistogram(T metric) {
        return null;
    }

    @Override
    public synchronized LatencyMetrics<T> flip() {
        return flip(clock.nanoTime());
    }

    @Override
    public synchronized LatencyMetrics<T> flip(long nanoTime) {
        return flip(nanoTime, latencyFactory.newLatency(clazz));
    }
}
