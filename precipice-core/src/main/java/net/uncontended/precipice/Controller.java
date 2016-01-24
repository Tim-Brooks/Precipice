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
import net.uncontended.precipice.concurrent.*;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.time.Clock;

public class Controller<T extends Enum<T> & Result> {
    private final PrecipiceSemaphore semaphore;
    private final ActionMetrics<T> actionMetrics;
    private final LatencyMetrics<T> latencyMetrics;
    private final CircuitBreaker circuitBreaker;
    private final String name;
    private final Clock clock;
    private final FinishingCallback<T> finishingCallback;
    private volatile boolean isShutdown = false;

    public Controller(String name, ControllerProperties<T> properties) {
        this(name, properties.semaphore(), properties.actionMetrics(), properties.latencyMetrics(),
                properties.circuitBreaker(), properties.clock());
    }

    public Controller(String name, PrecipiceSemaphore semaphore, ActionMetrics<T> actionMetrics,
                      LatencyMetrics<T> latencyMetrics, CircuitBreaker circuitBreaker, Clock clock) {
        this.semaphore = semaphore;
        this.actionMetrics = actionMetrics;
        this.latencyMetrics = latencyMetrics;
        this.circuitBreaker = circuitBreaker;
        this.name = name;
        this.clock = clock;
        this.circuitBreaker.setActionMetrics(actionMetrics);
        finishingCallback = new FinishingCallback<>(actionMetrics, circuitBreaker, latencyMetrics, semaphore, clock);
    }

    public Rejected acquirePermitOrGetRejectedReason() {
        if (isShutdown) {
            throw new IllegalStateException("Service has been shutdown.");
        }

        boolean isPermitAcquired = semaphore.acquirePermit(1);
        if (!isPermitAcquired) {
            return Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED;
        }

        if (!circuitBreaker.allowAction()) {
            semaphore.releasePermit(1);
            return Rejected.CIRCUIT_OPEN;
        }
        return null;
    }

    public <R> PrecipicePromise<T, R> acquirePermitAndGetPromise() {
        return acquirePermitAndGetPromise(null);
    }

    public <R> PrecipicePromise<T, R> acquirePermitAndGetPromise(PrecipicePromise<T, R> externalPromise) {
        Rejected rejected = acquirePermitOrGetRejectedReason();
        long startTime = clock.nanoTime();
        if (rejected != null) {
            actionMetrics.incrementRejectionCount(rejected, startTime);
            throw new RejectedActionException(rejected);
        }

        return getPromise(startTime, externalPromise);
    }

    public <R> PrecipicePromise<T, R> getPromise(long nanoTime) {
        return getPromise(nanoTime, null);
    }

    public <R> PrecipicePromise<T, R> getPromise(long nanoTime, PrecipicePromise<T, R> externalPromise) {
        NewEventual<T, R> promise = new NewEventual<>(nanoTime, externalPromise);
        promise.internalOnComplete(finishingCallback);
        return promise;
    }

    public <R> Completable<T, R> acquirePermitAndGetCompletableContext() {
        Rejected rejected = acquirePermitOrGetRejectedReason();
        long startTime = clock.nanoTime();
        if (rejected != null) {
            actionMetrics.incrementRejectionCount(rejected, startTime);
            throw new RejectedActionException(rejected);
        }

        return getCompletableContext(startTime);
    }

    public <R> Completable<T, R> getCompletableContext(long nanoTime) {
        CompletionContext<T, R> context = new CompletionContext<>(nanoTime);
        context.internalOnComplete(finishingCallback);
        return context;
    }

    public void shutdown() {
        isShutdown = true;
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

    public long remainingCapacity() {
        return semaphore.remainingCapacity();
    }

    public long pendingCount() {
        return semaphore.currentConcurrencyLevel();
    }

    public PrecipiceSemaphore getSemaphore() {
        return semaphore;
    }

    public Clock getClock() {
        return clock;
    }

    private static class FinishingCallback<T extends Enum<T> & Result> implements PrecipiceFunction<T, PerformingContext> {

        private final ActionMetrics<T> actionMetrics;
        private final CircuitBreaker circuitBreaker;
        private final LatencyMetrics<T> latencyMetrics;
        private final PrecipiceSemaphore semaphore;
        private final Clock clock;

        private FinishingCallback(ActionMetrics<T> actionMetrics, CircuitBreaker circuitBreaker,
                                  LatencyMetrics<T> latencyMetrics, PrecipiceSemaphore semaphore, Clock clock) {
            this.actionMetrics = actionMetrics;
            this.circuitBreaker = circuitBreaker;
            this.latencyMetrics = latencyMetrics;
            this.semaphore = semaphore;
            this.clock = clock;
        }

        @Override
        public void apply(T status, PerformingContext context) {
            long endTime = clock.nanoTime();
            actionMetrics.incrementMetricCount(status, endTime);
            circuitBreaker.informBreakerOfResult(status.isSuccess(), endTime);
            latencyMetrics.recordLatency(status, endTime - context.startNanos(), endTime);
            semaphore.releasePermit(1);
        }
    }
}
