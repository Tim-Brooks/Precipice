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
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.factories.Asynchronous;
import net.uncontended.precipice.result.TimeoutableResult;
import net.uncontended.precipice.threadpool.utils.PrecipiceExecutors;
import net.uncontended.precipice.threadpool.utils.TaskFactory;
import net.uncontended.precipice.timeout.TimeoutService;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class ThreadPoolService<Rejected extends Enum<Rejected>> implements Precipice<TimeoutableResult, Rejected> {
    private final ExecutorService executorService;
    private final TimeoutService timeoutService;
    private final GuardRail<TimeoutableResult, Rejected> guardRail;

    public ThreadPoolService(int poolSize, int queueSize, GuardRail<TimeoutableResult, Rejected> guardRail) {
        this(PrecipiceExecutors.threadPoolExecutor(guardRail.getName(), poolSize, queueSize), guardRail);
    }

    public ThreadPoolService(ExecutorService executorService, GuardRail<TimeoutableResult, Rejected> guardRail) {
        this.guardRail = guardRail;
        this.executorService = executorService;
        timeoutService = TimeoutService.DEFAULT_TIMEOUT_SERVICE;
    }

    @Override
    public GuardRail<TimeoutableResult, Rejected> guardRail() {
        return guardRail;
    }

    public <T> PrecipiceFuture<TimeoutableResult, T> submit(Callable<T> callable) {
        PrecipicePromise<TimeoutableResult, T> promise = Asynchronous.acquirePermitsAndPromise(guardRail, 1L);
        internalComplete(callable, promise);
        return promise.future();
    }

    public <T> PrecipiceFuture<TimeoutableResult, T> submit(Callable<T> callable, long millisTimeout) {
        PrecipicePromise<TimeoutableResult, T> promise = Asynchronous.acquirePermitsAndPromise(guardRail, 1L);
        internalComplete(callable, promise, millisTimeout);
        return promise.future();
    }

    public <T> void complete(Callable<T> callable, PrecipicePromise<TimeoutableResult, T> promise) {
        internalComplete(callable, promise);
    }

    public <T> void complete(Callable<T> callable, PrecipicePromise<TimeoutableResult, T> promise, long millisTimeout) {
        PrecipicePromise<TimeoutableResult, T> internalPromise = Asynchronous.acquirePermitsAndPromise(guardRail, 1L, promise);
        internalComplete(callable, internalPromise, millisTimeout);
    }

    private <T> void internalComplete(Callable<T> callable, PrecipicePromise<TimeoutableResult, T> promise) {
        executorService.execute(TaskFactory.createTask(callable, promise));
    }

    private <T> void internalComplete(Callable<T> callable, PrecipicePromise<TimeoutableResult, T> promise, long millisTimeout) {
        long startNanos = guardRail.getClock().nanoTime();
        long adjustedTimeout = TimeoutService.adjustTimeout(millisTimeout);
        CancellableTask<TimeoutableResult, T> task = TaskFactory.createTask(callable, promise);
        ThreadPoolTimeoutTask<T> timeoutTask = new ThreadPoolTimeoutTask<>(task);
        executorService.execute(task);
        timeoutService.scheduleTimeout(timeoutTask, adjustedTimeout, startNanos);
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
