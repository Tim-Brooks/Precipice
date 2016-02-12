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

import net.uncontended.precipice.RejectedException;
import net.uncontended.precipice.Failable;
import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.concurrent.Completable;
import net.uncontended.precipice.concurrent.Eventual;
import net.uncontended.precipice.concurrent.PrecipicePromise;

public class PromiseFactory<Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>> {

    private final GuardRail<Result, Rejected> guardRail;
    private final FinishingCallback<Result> finishingCallback;

    public PromiseFactory(GuardRail<Result, Rejected> guardRail) {
        this.guardRail = guardRail;
        finishingCallback = new FinishingCallback<>(guardRail);
    }

    public <R> Eventual<Result, R> acquirePermitsAndGetPromise(long number) {
        return acquirePermitsAndGetPromise(number, null);
    }

    public <R> Eventual<Result, R> acquirePermitsAndGetPromise(long number, PrecipicePromise<Result, R> externalPromise) {
        long startTime = guardRail.getClock().nanoTime();
        Rejected rejected = guardRail.acquirePermits(number, startTime);
        if (rejected != null) {
            throw new RejectedException(rejected);
        }

        return getPromise(number, startTime, externalPromise);
    }

    public <R> Eventual<Result, R> getPromise(long permitNumber, long nanoTime) {
        return getPromise(permitNumber, nanoTime, null);
    }

    public <R> Eventual<Result, R> getPromise(long permitNumber, long nanoTime, Completable<Result, R> externalCompletable) {
        Eventual<Result, R> promise = new Eventual<>(permitNumber, nanoTime, externalCompletable);
        promise.internalOnComplete(finishingCallback);
        return promise;
    }
}
