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

    public RejectionReason acquirePermitOrGetRejectedReason() {
        if (isShutdown) {
            throw new IllegalStateException("Service has been shutdown.");
        }

        boolean isPermitAcquired = semaphore.acquirePermit();
        if (!isPermitAcquired) {
            return RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED;
        }

        if (!circuitBreaker.allowAction()) {
            semaphore.releasePermit();
            return RejectionReason.CIRCUIT_OPEN;
        }
        return null;
    }

    public <R> PrecipicePromise<T, R> getPromise() {
        RejectionReason rejectionReason = acquirePermitOrGetRejectedReason();
        long startTime = System.nanoTime();
        if (rejectionReason != null) {
            actionMetrics.incrementRejectionCount(rejectionReason, startTime);
            throw new RejectedActionException(rejectionReason);
        }

        NewEventual<T, R> promise = new NewEventual<>(startTime);
        promise.internalOnComplete(new PrecipiceFunction<T, NewEventual<T, R>>() {
            @Override
            public void apply(T status, NewEventual<T, R> eventual) {
                long endTime = System.nanoTime();
                actionMetrics.incrementMetricCount(status);
                circuitBreaker.informBreakerOfResult(status.isSuccess());
                latencyMetrics.recordLatency(status, endTime - eventual.startNanos(), endTime);
                semaphore.releasePermit();
            }
        });
        return promise;
    }

    public PrecipiceSemaphore getSemaphore() {
        return semaphore;
    }

    public void shutdown() {
        isShutdown = true;
    }
}
