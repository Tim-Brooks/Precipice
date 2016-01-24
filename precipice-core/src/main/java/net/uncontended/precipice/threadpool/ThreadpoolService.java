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

public class ThreadPoolService implements Controllable {
    private final ExecutorService service;
    private final TimeoutService timeoutService;
    private final Controller<Status> controller;

    public ThreadPoolService(int poolSize, Controller<Status> controller) {
        this(PrecipiceExecutors.threadPoolExecutor(controller.getName(), poolSize,
                controller.getSemaphore().maxConcurrencyLevel()), controller);
    }

    public ThreadPoolService(ExecutorService service, Controller<Status> controller) {
        this.controller = controller;
        timeoutService = TimeoutService.defaultTimeoutService;
        this.service = service;
    }

    @Override
    public Controller<Status> controller() {
        return controller;
    }

    public <T> PrecipiceFuture<Status, T> submit(Callable<T> callable, long millisTimeout) {
        PrecipicePromise<Status, T> promise = controller.acquirePermitAndGetPromise();
        bypassBackPressureAndComplete(callable, promise, millisTimeout);
        return promise.future();
    }

    public <T> void complete(Callable<T> callable, PrecipicePromise<Status, T> promise, long millisTimeout) {
        PrecipicePromise<Status, T> internalPromise = controller.acquirePermitAndGetPromise(promise);
        bypassBackPressureAndComplete(callable, internalPromise, millisTimeout);
    }

    public <T> void bypassBackPressureAndComplete(Callable<T> callable, PrecipicePromise<Status, T> promise, long millisTimeout) {
        // TODO: Avoid multiple calls to system.nanotime()
        bypassBackPressureAndComplete(callable, promise, millisTimeout, controller.getClock().nanoTime());
    }

    public <T> void bypassBackPressureAndComplete(Callable<T> callable, PrecipicePromise<Status, T> promise,
                                                  long millisTimeout, long startNanos) {
        long adjustedTimeout = adjustTimeout(millisTimeout);
        ThreadPoolTask<T> task = new ThreadPoolTask<>(callable, promise, adjustedTimeout, startNanos);
        service.execute(task);
        timeoutService.scheduleTimeout(task);
    }

    public void shutdown() {
        controller.shutdown();
        service.shutdown();
    }

    private static long adjustTimeout(long millisTimeout) {
        return millisTimeout > TimeoutService.MAX_TIMEOUT_MILLIS ? TimeoutService.MAX_TIMEOUT_MILLIS : millisTimeout;
    }
}
