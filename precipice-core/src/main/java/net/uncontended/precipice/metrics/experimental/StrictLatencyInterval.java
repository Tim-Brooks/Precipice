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

import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;
import org.HdrHistogram.WriterReaderPhaser;

public class StrictLatencyInterval<T extends Enum<T>> implements LatencyMetrics<T>, Interval<LatencyMetrics<T>> {

    private final Clock clock = new SystemTime();
    private final Class<T> clazz;
    private final WriterReaderPhaser phaser = new WriterReaderPhaser();
    private volatile LatencyMetrics<T> live;

    public StrictLatencyInterval(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void recordLatency(T result, long number, long nanoLatency) {
        recordLatency(result, number, nanoLatency, clock.nanoTime());
    }

    @Override
    public void recordLatency(T result, long number, long nanoLatency, long nanoTime) {
        long permit = phaser.writerCriticalSectionEnter();
        try {
            live.recordLatency(result, number, nanoLatency, nanoTime);
        } finally {
            phaser.writerCriticalSectionExit(permit);
        }
    }

    @Override
    public Class<T> getMetricType() {
        return clazz;
    }

    @Override
    public synchronized LatencyMetrics<T> interval(LatencyMetrics<T> newMetrics) {
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
