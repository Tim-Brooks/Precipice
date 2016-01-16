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

import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.concurrent.NewEventual;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.concurrent.PrecipiceSemaphore;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;

public class NewController<T extends Enum<T> & Result> {
    private final PrecipiceSemaphore semaphore;
    private final ActionMetrics<T> actionMetrics;
    private final LatencyMetrics<T> latencyMetrics;
    private final CircuitBreaker circuitBreaker;
    private final String name;
    private final FinishingCallback<T> finishingCallback;
    private volatile boolean isShutdown = false;

    public NewController(String name, ControllerProperties<T> properties) {
        this(name, properties.semaphore(), properties.actionMetrics(), properties.latencyMetrics(),
                properties.circuitBreaker());
    }

    public NewController(String name, PrecipiceSemaphore semaphore, ActionMetrics<T> actionMetrics,
                         LatencyMetrics<T> latencyMetrics, CircuitBreaker circuitBreaker) {
        this.semaphore = semaphore;
        this.actionMetrics = actionMetrics;
        this.latencyMetrics = latencyMetrics;
        this.circuitBreaker = circuitBreaker;
        this.name = name;
        this.circuitBreaker.setActionMetrics(actionMetrics);
        finishingCallback = new FinishingCallback<>(actionMetrics, circuitBreaker, latencyMetrics, semaphore);
    }

    public String getName() {
        return name;
    }

    public ActionMetrics<T> getActionMetrics() {
        return actionMetrics;
    }

    public LatencyMetrics<T> getLatencyMetrics() {
        return latencyMetrics;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public int remainingCapacity() {
        return semaphore.remainingCapacity();
    }

    public int pendingCount() {
        return semaphore.currentConcurrencyLevel();
    }

    public Rejected acquirePermitOrGetRejectedReason() {
        if (isShutdown) {
            throw new IllegalStateException("Service has been shutdown.");
        }

        boolean isPermitAcquired = semaphore.acquirePermit();
        if (!isPermitAcquired) {
            return Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED;
        }

        if (!circuitBreaker.allowAction()) {
            semaphore.releasePermit();
            return Rejected.CIRCUIT_OPEN;
        }
        return null;
    }

    public <R> PrecipicePromise<T, R> getPromise() {
        return getPromise(null);
    }

    public <R> PrecipicePromise<T, R> getPromise(PrecipicePromise<T, R> externalPromise) {
        Rejected rejected = acquirePermitOrGetRejectedReason();
        long startTime = System.nanoTime();
        if (rejected != null) {
            actionMetrics.incrementRejectionCount(rejected, startTime);
            throw new RejectedActionException(rejected);
        }

        NewEventual<T, R> promise = new NewEventual<>(startTime, externalPromise);
        promise.internalOnComplete(finishingCallback);
        return promise;
    }

    public PrecipiceSemaphore getSemaphore() {
        return semaphore;
    }

    public void shutdown() {
        isShutdown = true;
    }

    private static class FinishingCallback<T extends Enum<T> & Result> implements PrecipiceFunction<T, PerformingContext> {

        private final ActionMetrics<T> actionMetrics;
        private final CircuitBreaker circuitBreaker;
        private final LatencyMetrics<T> latencyMetrics;
        private final PrecipiceSemaphore semaphore;

        private FinishingCallback(ActionMetrics<T> actionMetrics, CircuitBreaker circuitBreaker,
                                  LatencyMetrics<T> latencyMetrics, PrecipiceSemaphore semaphore) {
            this.actionMetrics = actionMetrics;
            this.circuitBreaker = circuitBreaker;
            this.latencyMetrics = latencyMetrics;
            this.semaphore = semaphore;
        }

        @Override
        public void apply(T status, PerformingContext context) {
            long endTime = System.nanoTime();
            actionMetrics.incrementMetricCount(status);
            circuitBreaker.informBreakerOfResult(status.isSuccess());
            latencyMetrics.recordLatency(status, endTime - context.startNanos(), endTime);
            semaphore.releasePermit();
        }
    }
}
