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

package net.uncontended.precipice.factories;

import net.uncontended.precipice.Failable;
import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.rejected.RejectedException;
import net.uncontended.precipice.Completable;
import net.uncontended.precipice.CompletionContext;

public class Synchronous {

    private Synchronous() {
    }

    public static <Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>, R> CompletionContext<Result, R>
    acquireSinglePermitAndCompletable(GuardRail<Result, Rejected> guardRail) {
        return acquirePermitsAndCompletable(guardRail, 1L, null);
    }

    public static <Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>, R> CompletionContext<Result, R>
    acquireSinglePermitAndCompletable(GuardRail<Result, Rejected> guardRail, Completable<Result, R> externalCompletable) {
        return acquirePermitsAndCompletable(guardRail, 1L, externalCompletable);
    }

    public static <Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>, R> CompletionContext<Result, R>
    acquirePermitsAndCompletable(GuardRail<Result, Rejected> guardRail, long number) {
        return acquirePermitsAndCompletable(guardRail, number, null);
    }

    public static <Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>, R> CompletionContext<Result, R>
    acquirePermitsAndCompletable(GuardRail<Result, Rejected> guardRail, long number, Completable<Result, R> externalCompletable) {
        long startTime = guardRail.getClock().nanoTime();
        Rejected rejected = guardRail.acquirePermits(number, startTime);
        if (rejected != null) {
            throw new RejectedException(rejected);
        }
        return getCompletable(guardRail, number, startTime, externalCompletable);
    }

    public static <Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>, R> CompletionContext<Result, R>
    getCompletable(GuardRail<Result, Rejected> guardRail, long permitNumber, long nanoTime) {
        return getCompletable(guardRail, permitNumber, nanoTime, null);
    }

    public static <Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>, R> CompletionContext<Result, R>
    getCompletable(GuardRail<Result, Rejected> guardRail, long permitNumber, long nanoTime, Completable<Result, R> externalCompletable) {
        CompletionContext<Result, R> completable = new CompletionContext<>(permitNumber, nanoTime, externalCompletable);
        completable.internalOnComplete(guardRail.releaseFunction());
        return completable;
    }

}
