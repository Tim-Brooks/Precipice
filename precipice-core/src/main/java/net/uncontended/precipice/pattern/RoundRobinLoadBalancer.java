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

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancer implements PatternStrategy {

    // TODO: The arrays can probably be cached
    // TODO: Also this is flawed because a failed service will drop its entire load on the next service

    private static final int FLIP_POINT = Integer.MAX_VALUE / 2;
    private final int flipPoint;
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
        this.flipPoint = (Integer.MAX_VALUE / 2) - maxAcquireAttempts;
    }

    @Override
    public Iterable<Integer> nextIndices() {
        int index = counter.getAndIncrement();

        if (index >= flipPoint) {
            resetCounter(index);
        }

        int[] orderToTry = new int[maxAcquireAttempts];
        for (int i = 0; i < maxAcquireAttempts; ++i) {
            orderToTry[i] = (index + i) % size;
        }
        shuffleTail(orderToTry);
        return new SingleReaderArrayIterable(orderToTry);
    }

    @Override
    public int acquireCount() {
        return 1;
    }

    private static void shuffleTail(int[] orderToTry) {
        int index;
        Random random = ThreadLocalRandom.current();
        for (int i = orderToTry.length - 1; i > 1; i--) {
            index = random.nextInt(i) + 1;
            if (index != i) {
                orderToTry[index] ^= orderToTry[i];
                orderToTry[i] ^= orderToTry[index];
                orderToTry[index] ^= orderToTry[i];
            }
        }
    }

    private void resetCounter(int start) {
        int index = start;
        for (; ; ) {
            if (index < flipPoint || counter.compareAndSet(index + 1, 0)) {
                break;
            }
            index = counter.get();
        }
    }
}
