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

package net.uncontended.precipice.backpressure;

import net.uncontended.precipice.Rejected;
import net.uncontended.precipice.Result;

import java.util.concurrent.atomic.AtomicLong;

public class BackPressureSemaphore implements BackPressure {

    private final AtomicLong permitsRemaining;
    private final int maxConcurrencyLevel;

    public BackPressureSemaphore(int maxConcurrencyLevel) {
        this.maxConcurrencyLevel = maxConcurrencyLevel;
        this.permitsRemaining = new AtomicLong(maxConcurrencyLevel);
    }

    @Override
    public Rejected acquirePermit(long units, long nanoTime) {
        for (; ; ) {
            long permitsRemaining = this.permitsRemaining.get();
            if (permitsRemaining > 0) {
                if (this.permitsRemaining.compareAndSet(permitsRemaining, permitsRemaining - 1)) {
                    return null;
                }
            } else {
                return Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED;
            }
        }
    }

    @Override
    public void releasePermit(long rateUnits, long nanoTime) {
        this.permitsRemaining.getAndIncrement();
    }

    @Override
    public void releasePermit(long rateUnits, Result result, long nanoTime) {
        this.permitsRemaining.getAndIncrement();
    }

    public long maxConcurrencyLevel() {
        return maxConcurrencyLevel;
    }

    public long remainingCapacity() {
        return permitsRemaining.get();
    }

    public long currentConcurrencyLevel() {
        return maxConcurrencyLevel - permitsRemaining.get();
    }
}