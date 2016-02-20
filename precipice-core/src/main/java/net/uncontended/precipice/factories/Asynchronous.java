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
import net.uncontended.precipice.concurrent.Completable;
import net.uncontended.precipice.concurrent.Eventual;

public class Asynchronous {

    private Asynchronous() {}

    public static <Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>, R> Eventual<Result, R>
    acquireSinglePermitAndPromise(GuardRail<Result, Rejected> guardRail) {
        return acquirePermitsAndPromise(guardRail, 1L, null);
    }

    public static <Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>, R> Eventual<Result, R>
    acquireSinglePermitAndPromise(GuardRail<Result, Rejected> guardRail, Completable<Result, R> externalCompletable) {
        return acquirePermitsAndPromise(guardRail, 1L, externalCompletable);
    }

    public static <Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>, R> Eventual<Result, R>
    acquirePermitsAndPromise(GuardRail<Result, Rejected> guardRail, long number) {
        return acquirePermitsAndPromise(guardRail, number, null);
    }

    public static <Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>, R> Eventual<Result, R>
    acquirePermitsAndPromise(GuardRail<Result, Rejected> guardRail, long number, Completable<Result, R> externalCompletable) {
        long startTime = guardRail.getClock().nanoTime();
        Rejected rejected = guardRail.acquirePermits(number, startTime);
        if (rejected != null) {
            throw new RejectedException(rejected);
        }
        return getPromise(guardRail, number, startTime, externalCompletable);
    }

    public static <Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>, R> Eventual<Result, R>
    getPromise(GuardRail<Result, Rejected> guardRail, long permitNumber, long nanoTime) {
        return getPromise(guardRail, permitNumber, nanoTime, null);
    }

    public static <Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>, R> Eventual<Result, R>
    getPromise(GuardRail<Result, Rejected> guardRail, long permitNumber, long nanoTime, Completable<Result, R> externalCompletable) {
        Eventual<Result, R> promise = new Eventual<>(permitNumber, nanoTime, externalCompletable);
        promise.internalOnComplete(guardRail.releaseFunction());
        return promise;
    }
}
