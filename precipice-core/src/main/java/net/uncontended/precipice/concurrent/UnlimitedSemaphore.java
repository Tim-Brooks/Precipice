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

public class UnlimitedSemaphore implements PrecipiceSemaphore {

    private final AtomicLong concurrencyLevel = new AtomicLong(0);

    @Override
    public boolean acquirePermit(long rateUnits) {
        concurrencyLevel.incrementAndGet();
        return true;
    }

    @Override
    public void releasePermit(long rateUnits) {
        concurrencyLevel.decrementAndGet();
    }

    @Override
    public long maxConcurrencyLevel() {
        return -1;
    }

    @Override
    public long remainingCapacity() {
        return -1;
    }

    @Override
    public long currentConcurrencyLevel() {
        return concurrencyLevel.get();
    }
}
