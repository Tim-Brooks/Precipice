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

import net.uncontended.precipice.backpressure.CompletableFactory;
import net.uncontended.precipice.concurrent.Completable;
import net.uncontended.precipice.timeout.PrecipiceTimeoutException;

import java.util.concurrent.Callable;

public class CallService implements Precipice<Status, Rejected> {

    private final GuardRail<Status, Rejected> guardRail;
    private final CompletableFactory<Status, Rejected> completableFactory;

    public CallService(GuardRail<Status, Rejected> guardRail) {
        this(guardRail, new CompletableFactory<>(guardRail));
    }

    public CallService(GuardRail<Status, Rejected> guardRail, CompletableFactory<Status, Rejected> completableFactory) {
        this.guardRail = guardRail;
        this.completableFactory = completableFactory;
    }

    @Override
    public GuardRail<Status, Rejected> guardRail() {
        return guardRail;
    }

    public <T> T call(Callable<T> callable) throws Exception {
        Completable<Status, T> completable = completableFactory.acquirePermitsAndGetCompletable(1L);

        try {
            T result = callable.call();
            completable.complete(Status.SUCCESS, result);
            return result;
        } catch (PrecipiceTimeoutException e) {
            completable.completeExceptionally(Status.TIMEOUT, e);
            throw e;
        } catch (Exception e) {
            completable.completeExceptionally(Status.ERROR, e);
            throw e;
        }
    }
}
