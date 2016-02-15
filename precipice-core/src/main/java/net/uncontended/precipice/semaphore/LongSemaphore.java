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

package net.uncontended.precipice.semaphore;

import net.uncontended.precipice.BackPressure;
import net.uncontended.precipice.Failable;
import net.uncontended.precipice.metrics.TotalCountMetrics;

import java.util.concurrent.atomic.AtomicLong;

public class LongSemaphore<Rejected extends Enum<Rejected>> implements BackPressure<Rejected>, PrecipiceSemaphore {

    private final AtomicLong permitsRemaining;
    private final int maxConcurrencyLevel;
    private final Rejected reason;

    public LongSemaphore(Rejected reason, int maxConcurrencyLevel) {
        this.maxConcurrencyLevel = maxConcurrencyLevel;
        this.reason = reason;
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
                return reason;
            }
        }
    }

    @Override
    public void releasePermit(long rateUnits, long nanoTime) {
        this.permitsRemaining.getAndIncrement();
    }

    @Override
    public void releasePermit(long rateUnits, Failable result, long nanoTime) {
        this.permitsRemaining.getAndIncrement();
    }

    @Override
    public <Result extends Enum<Result> & Failable> void registerResultMetrics(TotalCountMetrics<Result> metrics) {
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
