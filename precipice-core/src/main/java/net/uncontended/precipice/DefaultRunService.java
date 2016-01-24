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
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.timeout.PrecipiceTimeoutException;

public class DefaultRunService implements RunService {

    private final Controller<Status> controller;

    public DefaultRunService(Controller<Status> controller) {
        this.controller = controller;
    }

    @Override
    public <T> T run(final ResilientAction<T> action) throws Exception {
        Rejected rejected = controller.acquirePermitOrGetRejectedReason();
        if (rejected != null) {
            handleRejectedReason(rejected);
        }
        long nanoStart = System.nanoTime();
        try {
            T result = action.run();
            metricsAndBreakerFeedback(nanoStart, Status.SUCCESS);
            return result;
        } catch (PrecipiceTimeoutException e) {
            metricsAndBreakerFeedback(nanoStart, Status.TIMEOUT);
            throw e;
        } catch (Exception e) {
            metricsAndBreakerFeedback(nanoStart, Status.ERROR);
            throw e;
        } finally {
            controller.getSemaphore().releasePermit(1);
        }
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
    public long remainingCapacity() {
        return controller.remainingCapacity();
    }

    @Override
    public long pendingCount() {
        return controller.pendingCount();
    }

    @Override
    public void shutdown() {
        controller.shutdown();
    }

    private void handleRejectedReason(Rejected rejected) {
        if (rejected == Rejected.CIRCUIT_OPEN) {
            controller.getActionMetrics().incrementRejectionCount(Rejected.CIRCUIT_OPEN);
        } else if (rejected == Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED) {
            controller.getActionMetrics().incrementRejectionCount(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED);
        }
        throw new RejectedException(rejected);
    }

    private void metricsAndBreakerFeedback(long nanoStart, Status status) {
        long nanoTime = System.nanoTime();
        controller.getActionMetrics().incrementMetricCount(status, nanoTime);
        controller.getCircuitBreaker().informBreakerOfResult(status.isSuccess(), nanoTime);
        long latency = nanoTime - nanoStart;
        controller.getLatencyMetrics().recordLatency(status, latency, nanoTime);
    }
}
