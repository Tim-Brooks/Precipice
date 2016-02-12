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
import net.uncontended.precipice.concurrent.CompletionContext;

public class CompletableFactory<Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>> {

    private final GuardRail<Result, Rejected> guardRail;
    private final FinishingCallback<Result> finishingCallback;

    public CompletableFactory(GuardRail<Result, Rejected> guardRail) {
        this.guardRail = guardRail;
        finishingCallback = new FinishingCallback<>(guardRail);
    }

    public <T> Completable<Result, T> acquirePermitsAndGetCompletable(long number) {
        return acquirePermitsAndGetCompletable(number, guardRail.getClock().nanoTime());
    }

    public <T> Completable<Result, T> acquirePermitsAndGetCompletable(long number, long nanoTime) {
        Rejected rejected = guardRail.acquirePermits(number, nanoTime);
        if (rejected != null) {
            throw new RejectedException(rejected);
        }

        return getCompletable(number, nanoTime);
    }

    public <T> CompletionContext<Result, T> getCompletable(long permits, long nanoTime) {
        return getCompletable(permits, nanoTime, null);
    }

    public <T> CompletionContext<Result, T> getCompletable(long permits, long nanoTime, Completable<Result, T> completable) {
        CompletionContext<Result, T> context = new CompletionContext<>(permits, nanoTime, completable);
        context.internalOnComplete(finishingCallback);
        return context;
    }

}
