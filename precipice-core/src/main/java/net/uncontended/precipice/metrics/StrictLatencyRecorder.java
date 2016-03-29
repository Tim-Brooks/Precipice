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
import org.HdrHistogram.WriterReaderPhaser;

public class StrictLatencyRecorder<T extends Enum<T>> extends AbstractMetrics<T> implements LatencyMetrics<T>,
        Recorder<LatencyMetrics<T>> {

    private final Clock clock = new SystemTime();
    private final WriterReaderPhaser phaser = new WriterReaderPhaser();
    private final LatencyFactory latencyFactory;
    private final NoOpLatency<T> noOpLatency;
    private volatile LatencyMetrics<T> live;

    public StrictLatencyRecorder(Class<T> clazz) {
        this(clazz, Latency.atomicHDRHistogram());
    }

    public StrictLatencyRecorder(Class<T> clazz, LatencyFactory latencyFactory) {
        super(clazz);
        this.latencyFactory = latencyFactory;
        this.live = latencyFactory.newLatency(clazz, clock.nanoTime());
        this.noOpLatency = new NoOpLatency<>(clazz);
    }

    @Override
    public void record(T result, long number, long nanoLatency) {
        record(result, number, nanoLatency, clock.nanoTime());
    }

    @Override
    public void record(T result, long number, long nanoLatency, long nanoTime) {
        long permit = phaser.writerCriticalSectionEnter();
        try {
            live.record(result, number, nanoLatency, nanoTime);
        } finally {
            phaser.writerCriticalSectionExit(permit);
        }
    }

    @Override
    public PrecipiceHistogram getHistogram(T metric) {
        return noOpLatency.getHistogram(metric);
    }
    
    public LatencyMetrics<T> current() {
        return live;
    }

    @Override
    public synchronized LatencyMetrics<T> flip() {
        return flip(clock.nanoTime());
    }

    @Override
    public synchronized LatencyMetrics<T> flip(long nanoTime) {
        return flip(nanoTime, latencyFactory.newLatency(clazz, nanoTime));
    }

    @Override
    public synchronized LatencyMetrics<T> flip(long nanoTime, LatencyMetrics<T> newMetrics) {
        phaser.readerLock();
        try {
            LatencyMetrics<T> oldLive = live;
            live = newMetrics;
            phaser.flipPhase(500000L);
            return oldLive;
        } finally {
            phaser.readerUnlock();
        }
    }
}
