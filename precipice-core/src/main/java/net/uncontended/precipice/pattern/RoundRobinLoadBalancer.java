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

package net.uncontended.precipice.pattern;

import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements PatternStrategy {

    // TODO: The arrays can probably be cached
    // TODO: Also this is flawed because a failed service will drop its entire load on the next service

    private static final int FLIP_POINT = Integer.MAX_VALUE / 2;
    private final int size;
    private final int maxAcquireAttempts;
    private final AtomicInteger counter;

    public RoundRobinLoadBalancer(int size) {
        this(size, size);
    }

    public RoundRobinLoadBalancer(int size, int maxAcquireAttempts) {
        this(size, maxAcquireAttempts, new AtomicInteger(0));
    }

    public RoundRobinLoadBalancer(int size, int maxAcquireAttempts, AtomicInteger counter) {
        this.size = size;
        this.maxAcquireAttempts = maxAcquireAttempts;
        this.counter = counter;
    }

    @Override
    public int[] nextIndices() {
        int index = counter.getAndIncrement();

        if (index >= FLIP_POINT) {
            resetCounter(index);
        }

        int[] indices = new int[maxAcquireAttempts];
        for (int i = 0; i < maxAcquireAttempts; ++i) {
            indices[i] = (index + i) % size;
        }
        return indices;
    }

    @Override
    public int attemptCount() {
        return 1;
    }

    private void resetCounter(int start) {
        int index = start;
        for (; ; ) {
            if (index < FLIP_POINT || counter.compareAndSet(index + 1, 0)) {
                break;
            }
            index = counter.get();

        }
    }
}
