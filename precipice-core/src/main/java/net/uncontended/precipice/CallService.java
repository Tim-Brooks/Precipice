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

package net.uncontended.precipice;

import net.uncontended.precipice.concurrent.Completable;
import net.uncontended.precipice.factories.Synchronous;
import net.uncontended.precipice.result.TimeoutableResult;
import net.uncontended.precipice.timeout.PrecipiceTimeoutException;

import java.util.concurrent.Callable;

public class CallService<Rejected extends Enum<Rejected>> implements Precipice<TimeoutableResult, Rejected> {

    private final GuardRail<TimeoutableResult, Rejected> guardRail;

    public CallService(GuardRail<TimeoutableResult, Rejected> guardRail) {
        this.guardRail = guardRail;
    }

    @Override
    public GuardRail<TimeoutableResult, Rejected> guardRail() {
        return guardRail;
    }

    public <T> T call(Callable<T> callable) throws Exception {
        return call(callable, 1L);
    }

    public <T> T call(Callable<T> callable, long permitNumber) throws Exception {
        Completable<TimeoutableResult, T> completable = Synchronous.acquirePermitsAndCompletable(guardRail, permitNumber);

        try {
            T result = callable.call();
            completable.complete(TimeoutableResult.SUCCESS, result);
            return result;
        } catch (PrecipiceTimeoutException e) {
            completable.completeExceptionally(TimeoutableResult.TIMEOUT, e);
            throw e;
        } catch (Exception e) {
            completable.completeExceptionally(TimeoutableResult.ERROR, e);
            throw e;
        }
    }
}
