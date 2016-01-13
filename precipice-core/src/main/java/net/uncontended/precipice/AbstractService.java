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
import net.uncontended.precipice.metrics.LatencyMetrics;

public abstract class AbstractService implements Service {
    protected final PrecipiceSemaphore semaphore;
    protected final ActionMetrics<?> actionMetrics;
    protected final LatencyMetrics latencyMetrics;
    protected final CircuitBreaker circuitBreaker;
    protected final String name;
    protected volatile boolean isShutdown = false;

    protected AbstractService(String name, CircuitBreaker circuitBreaker, ActionMetrics<?> actionMetrics,
                              LatencyMetrics latencyMetrics, PrecipiceSemaphore semaphore) {
        this.name = name;
        this.circuitBreaker = circuitBreaker;
        this.actionMetrics = actionMetrics;
        this.latencyMetrics = latencyMetrics;
        this.semaphore = semaphore;
        this.circuitBreaker.setActionMetrics(actionMetrics);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ActionMetrics<?> getActionMetrics() {
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
        return semaphore.remainingCapacity();

    }

    @Override
    public int pendingCount() {
        return semaphore.currentConcurrencyLevel();
    }

    protected RejectionReason acquirePermitOrGetRejectedReason() {
        if (isShutdown) {
            return RejectionReason.SERVICE_SHUTDOWN;
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
}
