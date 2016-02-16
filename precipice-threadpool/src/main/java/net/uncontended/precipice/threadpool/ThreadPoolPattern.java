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
package net.uncontended.precipice.threadpool;

import net.uncontended.precipice.*;
import net.uncontended.precipice.factories.PromiseFactory;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.pattern.Pattern;
import net.uncontended.precipice.pattern.PatternAction;
import net.uncontended.precipice.pattern.PatternStrategy;
import net.uncontended.precipice.pattern.Sequence;
import net.uncontended.precipice.timeout.TimeoutService;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class ThreadPoolPattern<C> implements Precipice<TimeoutableResult, Rejected> {

    private final GuardRail<TimeoutableResult, Rejected> guardRail;
    private final Pattern<TimeoutableResult, ThreadPoolService<?>> pattern;
    private final Map<ThreadPoolService<?>, C> serviceToContext;


    public ThreadPoolPattern(Map<ThreadPoolService<?>, C> serviceToContext, GuardRail<TimeoutableResult, Rejected> guardRail,
                             PatternStrategy strategy) {
        this(serviceToContext, guardRail, new Pattern<>(serviceToContext.keySet(), strategy));
    }

    public ThreadPoolPattern(Map<ThreadPoolService<?>, C> serviceToContext, GuardRail<TimeoutableResult, Rejected> guardRail,
                             Pattern<TimeoutableResult, ThreadPoolService<?>> pattern) {
        this.serviceToContext = serviceToContext;
        this.guardRail = guardRail;
        this.pattern = pattern;
    }

    @Override
    public GuardRail<TimeoutableResult, Rejected> guardRail() {
        return guardRail;
    }

    public <T> PrecipiceFuture<TimeoutableResult, T> submit(final PatternAction<T, C> action, long millisTimeout) {
        long nanoTime = acquirePermit();

        Sequence<ThreadPoolService<?>> services = pattern.getPrecipices(1L, nanoTime);

        if (services.isEmpty()) {
            return handleAllReject(nanoTime);
        }

        PrecipicePromise<TimeoutableResult, T> promise = PromiseFactory.getPromise(guardRail, 1L, nanoTime);
        long adjustedTimeout = TimeoutService.adjustTimeout(millisTimeout);
        for (ThreadPoolService<?> service : services) {
            PrecipicePromise<TimeoutableResult, T> internal = PromiseFactory.getPromise(service.guardRail(), 1L, nanoTime, promise);

            final C context = serviceToContext.get(service);
            ExecutorService executor = service.getExecutor();
            TimeoutService timeoutService = service.getTimeoutService();

            Callable<T> callable = new CallableWithContext<>(action, context);
            ThreadPoolTask<T> task = new ThreadPoolTask<>(callable, internal, adjustedTimeout, nanoTime);
            executor.execute(task);
            timeoutService.scheduleTimeout(task);
        }

        return promise.future();
    }

    private <T> PrecipiceFuture<TimeoutableResult, T> handleAllReject(long nanoTime) {
        guardRail.releasePermitsWithoutResult(1L, nanoTime);
        guardRail.getRejectedMetrics().incrementMetricCount(Rejected.ALL_SERVICES_REJECTED, nanoTime);
        throw new RejectedException(Rejected.ALL_SERVICES_REJECTED);
    }

    private long acquirePermit() {
        long nanoTime = guardRail.getClock().nanoTime();
        Rejected rejected = guardRail.acquirePermits(1L, nanoTime);
        if (rejected != null) {
            throw new RejectedException(rejected);
        }
        return nanoTime;
    }

    public void shutdown() {
        for (ThreadPoolService service : serviceToContext.keySet()) {
            service.shutdown();
        }
    }
}
