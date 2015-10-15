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
import net.uncontended.precipice.concurrent.PrecipiceSemaphore;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.DefaultLatencyMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.metrics.Metric;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractService implements Service {
    protected final PrecipiceSemaphore semaphore;
    protected final AtomicBoolean isShutdown;
    protected final ActionMetrics actionMetrics;
    protected final LatencyMetrics latencyMetrics = new DefaultLatencyMetrics();
    protected final CircuitBreaker circuitBreaker;
    protected final String name;

    public AbstractService(String name, CircuitBreaker circuitBreaker, ActionMetrics actionMetrics, PrecipiceSemaphore
            semaphore) {
        this(name, circuitBreaker, actionMetrics, semaphore, new AtomicBoolean(false));
    }

    public AbstractService(String name, CircuitBreaker circuitBreaker, ActionMetrics actionMetrics, PrecipiceSemaphore
            semaphore, AtomicBoolean isShutdown) {
        this.name = name;
        this.circuitBreaker = circuitBreaker;
        this.actionMetrics = actionMetrics;
        this.semaphore = semaphore;
        this.isShutdown = isShutdown;
        this.circuitBreaker.setActionMetrics(actionMetrics);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ActionMetrics getActionMetrics() {
        return actionMetrics;
    }

    @Override
    public LatencyMetrics getLatencyMetrics() {
        return latencyMetrics;
    }

    @Override
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    @Override
    public int remainingCapacity() {
        return semaphore.maxConcurrencyLevel() - semaphore.currentConcurrencyLevel();

    }

    @Override
    public int currentlyPending() {
        return semaphore.currentConcurrencyLevel();
    }

    protected void acquirePermitOrRejectIfActionNotAllowed() {
        if (isShutdown.get()) {
            throw new RejectedActionException(RejectionReason.SERVICE_SHUTDOWN);
        }

        boolean isPermitAcquired = semaphore.acquirePermit();
        if (!isPermitAcquired) {
            actionMetrics.incrementMetricCount(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED);
            throw new RejectedActionException(RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED);
        }

        if (!circuitBreaker.allowAction()) {
            actionMetrics.incrementMetricCount(Metric.CIRCUIT_OPEN);
            semaphore.releasePermit();
            throw new RejectedActionException(RejectionReason.CIRCUIT_OPEN);
        }
    }
}
