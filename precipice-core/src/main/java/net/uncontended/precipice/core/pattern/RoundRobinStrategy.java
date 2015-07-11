/*
 * Copyright 2014 Timothy Brooks
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

package net.uncontended.precipice.core.pattern;

import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinStrategy implements LoadBalancerStrategy {

    private static final int FLIP_POINT = Integer.MAX_VALUE / 2;
    private final int size;
    private final AtomicInteger counter;

    public RoundRobinStrategy(int size) {
        this(size, new AtomicInteger(0));
    }

    public RoundRobinStrategy(int size, AtomicInteger counter) {
        this.size = size;
        this.counter = counter;
    }

    @Override
    public int nextExecutorIndex() {
        int index = counter.getAndIncrement();

        if (index >= FLIP_POINT) {
            resetCounter(index);
        }

        return index % size;
    }

    private void resetCounter(int start) {
        int index = start;
        for (; ; ) {
            if (index < FLIP_POINT) {
                break;
            } else if (counter.compareAndSet(index + 1, 0)) {
                break;
            }
            index = counter.get();

        }
    }
}
