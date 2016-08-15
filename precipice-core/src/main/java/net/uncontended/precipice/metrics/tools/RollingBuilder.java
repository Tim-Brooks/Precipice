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

import java.util.concurrent.TimeUnit;

public abstract class RollingBuilder<T, S> {

    protected Clock clock = SystemTime.getInstance();
    protected int buckets = -1;
    protected long nanosPerBucket = -1;
    protected Allocator<T> allocator;

    public RollingBuilder<T, S> bucketCount(int buckets) {
        this.buckets = buckets;
        return this;
    }

    public RollingBuilder<T, S> bucketResolution(long duration, TimeUnit unit) {
        nanosPerBucket = unit.toNanos(duration);
        return this;
    }

    public RollingBuilder<T, S> withClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public RollingBuilder<T, S> withAllocator(Allocator<T> allocator) {
        this.allocator = allocator;
        return this;
    }

    public abstract S build();

    protected RollingMetrics<T> buildRollingMetrics() {
        if (allocator == null) {
            throw new IllegalArgumentException("Allocator cannot be null.");
        }
        if (buckets < 0) {
            throw new IllegalArgumentException("Number of buckets tracked must be positive. Found: " + buckets);
        }

        if (nanosPerBucket <= 0) {
            throw new IllegalArgumentException("Nano seconds per bucket must be greater than 0. Found: " + nanosPerBucket);
        }

        CircularBuffer<T> circularBuffer = new CircularBuffer<>(buckets, nanosPerBucket, clock.nanoTime());

        return new RollingMetrics<>(allocator, circularBuffer, clock);
    }

}
