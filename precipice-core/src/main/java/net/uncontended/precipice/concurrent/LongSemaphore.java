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

package net.uncontended.precipice.concurrent;

import java.util.concurrent.atomic.AtomicLong;

public class LongSemaphore implements PrecipiceSemaphore {

    private final AtomicLong permitsRemaining;
    private final int maxConcurrencyLevel;

    public LongSemaphore(int maxConcurrencyLevel) {
        this.maxConcurrencyLevel = maxConcurrencyLevel;
        this.permitsRemaining = new AtomicLong(maxConcurrencyLevel);
    }

    @Override
    public boolean acquirePermit(long rateUnit) {
        for (; ; ) {
            long permitsRemaining = this.permitsRemaining.get();
            if (permitsRemaining > 0) {
                if (this.permitsRemaining.compareAndSet(permitsRemaining, permitsRemaining - 1)) {
                    return true;
                }
            } else {
                return false;
            }
        }
    }

    @Override
    public void releasePermit(long rateUnit) {
        this.permitsRemaining.getAndIncrement();
    }

    @Override
    public long maxConcurrencyLevel() {
        return maxConcurrencyLevel;
    }

    @Override
    public long remainingCapacity() {
        return permitsRemaining.get();
    }

    @Override
    public long currentConcurrencyLevel() {
        return maxConcurrencyLevel - permitsRemaining.get();
    }
}
