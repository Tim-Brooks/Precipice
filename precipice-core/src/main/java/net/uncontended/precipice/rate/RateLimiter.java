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

package net.uncontended.precipice.rate;

import net.uncontended.precipice.BackPressure;
import net.uncontended.precipice.Failable;
import net.uncontended.precipice.metrics.CountMetrics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class RateLimiter<Rejected extends Enum<Rejected>> implements BackPressure<Rejected> {

    private final Rejected rejectedReason;
    private final long allowedPerPeriod;
    private final AtomicLong count = new AtomicLong(0);
    private final AtomicLong rolloverTime;
    private final long nanoDuration;

    public RateLimiter(Rejected rejectedReason, long allowedPerPeriod, long duration, TimeUnit timeUnit) {
        this.rejectedReason = rejectedReason;
        this.allowedPerPeriod = allowedPerPeriod;
        this.nanoDuration = timeUnit.toNanos(duration);
        this.rolloverTime = new AtomicLong(System.nanoTime() + nanoDuration);
    }

    @Override
    public Rejected acquirePermit(long number, long nanoTime) {
        adjustTime(nanoTime);

        for (; ; ) {
            long currentCount = count.get();
            long proposedCount = currentCount + number;
            if (proposedCount > allowedPerPeriod) {
                return rejectedReason;
            } else if (count.compareAndSet(currentCount, proposedCount)) {
                return null;
            }
        }
    }

    private void adjustTime(long nanoTime) {
        for (; ; ) {
            long localRolloverTime = rolloverTime.get();
            if (localRolloverTime > nanoTime) {
                return;
            } else if (rolloverTime.compareAndSet(localRolloverTime, nanoTime + nanoDuration)) {
                count.set(0);
            }
        }
    }

    @Override
    public void releasePermit(long number, long nanoTime) {
    }

    @Override
    public void releasePermit(long number, Failable result, long nanoTime) {
    }

    @Override
    public <Result extends Enum<Result> & Failable> void registerResultMetrics(CountMetrics<Result> metrics) {

    }
}
