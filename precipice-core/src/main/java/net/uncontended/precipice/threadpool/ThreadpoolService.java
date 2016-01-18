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

import net.uncontended.precipice.NewController;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.timeout.TimeoutService;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class ThreadpoolService {
    private final ExecutorService service;
    private final TimeoutService timeoutService;
    private final NewController<Status> controller;

    public ThreadpoolService(ExecutorService service, NewController<Status> controller) {
        this.controller = controller;
        timeoutService = TimeoutService.defaultTimeoutService;
        this.service = service;
    }

    public <T> PrecipiceFuture<Status, T> submit(Callable<T> action, long millisTimeout) {
        PrecipicePromise<Status, T> promise = controller.acquirePermitAndGetPromise();
        internalComplete(action, promise, millisTimeout);
        return promise.future();
    }

    public <T> void complete(Callable<T> action, PrecipicePromise<Status, T> promise, long millisTimeout) {
        PrecipicePromise<Status, T> internalPromise = controller.acquirePermitAndGetPromise(promise);
        internalComplete(action, internalPromise, millisTimeout);
    }

    public NewController<Status> controller() {
        return controller;
    }

    private <T> void internalComplete(Callable<T> action, PrecipicePromise<Status, T> promise, long millisTimeout) {
        long adjustedTimeout = millisTimeout > TimeoutService.MAX_TIMEOUT_MILLIS ? TimeoutService.MAX_TIMEOUT_MILLIS : millisTimeout;
        NewResilientTask<T> task = new NewResilientTask<>(action, promise, adjustedTimeout, System.nanoTime());
        service.execute(task);
        timeoutService.scheduleTimeout(task);
    }
}
