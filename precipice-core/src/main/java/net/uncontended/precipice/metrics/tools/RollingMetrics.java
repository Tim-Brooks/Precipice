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

package net.uncontended.precipice.metrics.tools;

import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;

public class RollingMetrics<T> implements Rolling<T> {

    private final Allocator<T> allocator;
    private final Clock clock;
    private final CircularBuffer<T> buffer;

    public RollingMetrics(Allocator<T> allocator, CircularBuffer<T> buffer) {
        this(allocator, buffer, new SystemTime());
    }

    public RollingMetrics(Allocator<T> allocator, CircularBuffer<T> buffer, Clock clock) {
        this.buffer = buffer;
        this.clock = clock;
        this.allocator = allocator;
    }

    public T current() {
        return buffer.getSlot(clock.nanoTime());
    }

    public T current(long nanoTime) {
        T current = buffer.getSlot(nanoTime);
        if (current == null) {
            current = buffer.putOrGet(nanoTime, allocator.allocateNew());
        }
        return current;
    }

    public IntervalIterator<T> intervals() {
        return intervals(clock.nanoTime());
    }

    public IntervalIterator<T> intervals(long nanoTime) {
        return buffer.intervals(nanoTime, null);
    }
}
