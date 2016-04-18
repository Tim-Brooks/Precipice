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

import net.uncontended.precipice.metrics.Allocator;
import net.uncontended.precipice.metrics.CircularBuffer;
import net.uncontended.precipice.metrics.IntervalIterator;
import net.uncontended.precipice.time.Clock;

public class NewRoller<T> implements NewMetrics<T> {

    private final Allocator<T> allocator;
    private final Clock clock;
    private final T total;
    private final CircularBuffer<T> buffer;

    public NewRoller(T total, Allocator<T> allocator, CircularBuffer<T> buffer, Clock clock) {
        this.total = total;
        this.buffer = buffer;
        this.clock = clock;
        this.allocator = allocator;
    }

    @Override
    public T current() {
        return buffer.getSlot(clock.nanoTime());
    }

    @Override
    public T current(long nanoTime) {
        T current = buffer.getSlot(nanoTime);
        if (current == null) {
            T newCurrent = allocator.allocateNew();
            // TODO: Consider whether null is the correct return value
            buffer.putOrGet(nanoTime, newCurrent);
            current = allocator.allocateNew();
        }
        return current;
    }

    @Override
    public T total() {
        return total;
    }

    public IntervalIterator<T> intervals() {
        return intervals(clock.nanoTime());
    }

    public IntervalIterator<T> intervals(long nanoTime) {
        return buffer.intervals(nanoTime, null);
    }
}
