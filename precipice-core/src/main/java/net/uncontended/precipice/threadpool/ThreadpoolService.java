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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class ThreadpoolService implements Controllable {
    private final ExecutorService service;
    private final TimeoutService timeoutService;
    private final Controller<Status> controller;

    public ThreadpoolService(ExecutorService service, Controller<Status> controller) {
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
        bypassBackpressureAndComplete(callable, promise, millisTimeout);
        return promise.future();
    }

    public <T> void complete(Callable<T> callable, PrecipicePromise<Status, T> promise, long millisTimeout) {
        PrecipicePromise<Status, T> internalPromise = controller.acquirePermitAndGetPromise(promise);
        bypassBackpressureAndComplete(callable, internalPromise, millisTimeout);
    }

    public <T> void bypassBackpressureAndComplete(Callable<T> callable, PrecipicePromise<Status, T> promise, long millisTimeout) {
        long adjustedTimeout = adjustTimeout(millisTimeout);
        NewResilientTask<T> task = new NewResilientTask<>(callable, promise, adjustedTimeout, System.nanoTime());
        service.execute(task);
        timeoutService.scheduleTimeout(task);
    }

    private static long adjustTimeout(long millisTimeout) {
        return millisTimeout > TimeoutService.MAX_TIMEOUT_MILLIS ? TimeoutService.MAX_TIMEOUT_MILLIS : millisTimeout;
    }
}
