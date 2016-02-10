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

import net.uncontended.precipice.Controllable;
import net.uncontended.precipice.Controller;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.timeout.TimeoutService;
import net.uncontended.precipice.utils.PrecipiceExecutors;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class ThreadPoolService implements Controllable<Status> {
    private final ExecutorService executorService;
    private final TimeoutService timeoutService;
    private final Controller<Status> controller;

    public ThreadPoolService(int poolSize, Controller<Status> controller) {
        this(PrecipiceExecutors.threadPoolExecutor(controller.getName(), poolSize,
                controller.getSemaphore().maxConcurrencyLevel()), controller);
    }

    public ThreadPoolService(ExecutorService executorService, Controller<Status> controller) {
        this.controller = controller;
        this.executorService = executorService;
        timeoutService = TimeoutService.defaultTimeoutService;
    }

    @Override
    public Controller<Status> controller() {
        return controller;
    }

    public <T> PrecipiceFuture<Status, T> submit(Callable<T> callable, long millisTimeout) {
        PrecipicePromise<Status, T> promise = controller.acquirePermitAndGetPromise();
        internalComplete(callable, promise, millisTimeout);
        return promise.future();
    }

    public <T> void complete(Callable<T> callable, PrecipicePromise<Status, T> promise, long millisTimeout) {
        PrecipicePromise<Status, T> internalPromise = controller.acquirePermitAndGetPromise(promise);
        internalComplete(callable, internalPromise, millisTimeout);
    }

    private <T> void internalComplete(Callable<T> callable, PrecipicePromise<Status, T> promise, long millisTimeout) {
        long startNanos = controller.getClock().nanoTime();
        long adjustedTimeout = TimeoutService.adjustTimeout(millisTimeout);
        ThreadPoolTask<T> task = new ThreadPoolTask<>(callable, promise, adjustedTimeout, startNanos);
        executorService.execute(task);
        timeoutService.scheduleTimeout(task);
    }

    public ExecutorService getExecutor() {
        return executorService;
    }

    public TimeoutService getTimeoutService() {
        return timeoutService;
    }

    public void shutdown() {
        controller.shutdown();
        executorService.shutdown();
    }

}
