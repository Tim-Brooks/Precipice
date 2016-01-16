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

package net.uncontended.precipice;

import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.concurrent.ResilientTask;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.timeout.TimeoutService;

import java.util.concurrent.ExecutorService;

public class DefaultAsyncService implements AsyncService {
    private final ExecutorService service;
    private final TimeoutService timeoutService;
    private final NewController<Status> controller;

    public DefaultAsyncService(ExecutorService service, NewController<Status> controller) {
        this.controller = controller;
        timeoutService = TimeoutService.defaultTimeoutService;
        this.service = service;
    }

    @Override
    public <T> PrecipiceFuture<Status, T> submit(ResilientAction<T> action, long millisTimeout) {
        PrecipicePromise<Status, T> promise = controller.getPromise();
        internalComplete(action, promise, millisTimeout);
        return promise.future();
    }

    @Override
    public <T> void complete(ResilientAction<T> action, PrecipicePromise<Status, T> promise, long millisTimeout) {
        PrecipicePromise<Status, T> internalPromise = controller.getPromise(promise);
        internalComplete(action, internalPromise, millisTimeout);
    }

    private <T> void internalComplete(ResilientAction<T> action, PrecipicePromise<Status, T> promise, long millisTimeout) {
        long adjustedTimeout = millisTimeout > TimeoutService.MAX_TIMEOUT_MILLIS ? TimeoutService.MAX_TIMEOUT_MILLIS : millisTimeout;
        ResilientTask<T> task = new ResilientTask<>(action, promise, adjustedTimeout, System.nanoTime());
        service.execute(task);
        timeoutService.scheduleTimeout(task);
    }

    @Override
    public String getName() {
        return controller.getName();
    }

    @Override
    public ActionMetrics<Status> getActionMetrics() {
        return controller.getActionMetrics();
    }

    @Override
    public LatencyMetrics<Status> getLatencyMetrics() {
        return controller.getLatencyMetrics();
    }

    @Override
    public CircuitBreaker getCircuitBreaker() {
        return controller.getCircuitBreaker();
    }

    @Override
    public int remainingCapacity() {
        return controller.remainingCapacity();
    }

    @Override
    public int pendingCount() {
        return controller.pendingCount();
    }

    @Override
    public void shutdown() {
        controller.shutdown();
        service.shutdown();
    }
}
