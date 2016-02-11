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

package net.uncontended.precipice.threadpool;

import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.Precipice;
import net.uncontended.precipice.Rejected;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.threadpool.utils.PrecipiceExecutors;
import net.uncontended.precipice.timeout.TimeoutService;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class ThreadPoolService implements Precipice<Status, Rejected> {
    private final ExecutorService executorService;
    private final TimeoutService timeoutService;
    private final GuardRail<Status, Rejected> guardRail;

    public ThreadPoolService(int poolSize, int queueSize, GuardRail<Status, Rejected> guardRail) {
        // TODO: Need to figure out max concurrency
        this(PrecipiceExecutors.threadPoolExecutor(guardRail.getName(), poolSize, queueSize), guardRail);
    }

    public ThreadPoolService(ExecutorService executorService, GuardRail<Status, Rejected> guardRail) {
        this.guardRail = guardRail;
        this.executorService = executorService;
        timeoutService = TimeoutService.defaultTimeoutService;
    }

    @Override
    public GuardRail<Status, Rejected> guardRail() {
        return guardRail;
    }

    public <T> PrecipiceFuture<Status, T> submit(Callable<T> callable, long millisTimeout) {
        PrecipicePromise<Status, T> promise = guardRail.acquirePermitAndGetPromise(1L);
        internalComplete(callable, promise, millisTimeout);
        return promise.future();
    }

    public <T> void complete(Callable<T> callable, PrecipicePromise<Status, T> promise, long millisTimeout) {
        PrecipicePromise<Status, T> internalPromise = guardRail.acquirePermitAndGetPromise(1L, promise);
        internalComplete(callable, internalPromise, millisTimeout);
    }

    private <T> void internalComplete(Callable<T> callable, PrecipicePromise<Status, T> promise, long millisTimeout) {
        long startNanos = guardRail.getClock().nanoTime();
        long adjustedTimeout = TimeoutService.adjustTimeout(millisTimeout);
        ThreadPoolTask<T> task = new ThreadPoolTask<>(callable, promise, adjustedTimeout, startNanos);
        executorService.execute(task);
        if (task.canTimeout()) {
            timeoutService.scheduleTimeout(task);
        }
    }

    public ExecutorService getExecutor() {
        return executorService;
    }

    public TimeoutService getTimeoutService() {
        return timeoutService;
    }

    public void shutdown() {
        guardRail.shutdown();
        executorService.shutdown();
    }

}
