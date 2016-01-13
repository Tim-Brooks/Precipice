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

import net.uncontended.precipice.concurrent.Eventual;
import net.uncontended.precipice.concurrent.PrecipiceFuture;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.concurrent.ResilientTask;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.timeout.TimeoutService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public class DefaultAsyncService extends AbstractService implements AsyncService {
    private final ExecutorService service;
    private final TimeoutService timeoutService;
    private final ActionMetrics<SuperImpl> actionMetrics;

    public DefaultAsyncService(String name, ExecutorService service, ServiceProperties properties) {
        super(name, properties.circuitBreaker(), properties.actionMetrics(), properties.latencyMetrics(), properties.semaphore());
        actionMetrics = (ActionMetrics<SuperImpl>) properties.actionMetrics();
        timeoutService = properties.timeoutService();
        this.service = service;
    }

    @Override
    public <T> PrecipiceFuture<SuperImpl, T> submit(ResilientAction<T> action, long millisTimeout) {
        Eventual<SuperImpl, T> promise = new Eventual<>();
        complete(action, promise, millisTimeout);
        return promise;
    }

    @Override
    public <T> void complete(ResilientAction<T> action, PrecipicePromise<SuperImpl, T> promise, long millisTimeout) {
        RejectionReason rejectionReason = acquirePermitOrGetRejectedReason();
        if (rejectionReason != null) {
            handleRejectedReason(rejectionReason);
        }
        try {
            ResilientTask<T> task = new ResilientTask<>(actionMetrics, latencyMetrics, semaphore, circuitBreaker,
                    action, promise, millisTimeout > MAX_TIMEOUT_MILLIS ? MAX_TIMEOUT_MILLIS : millisTimeout,
                    System.nanoTime());
            service.execute(task);
            timeoutService.scheduleTimeout(task);

        } catch (RejectedExecutionException e) {
            actionMetrics.incrementMetricCount(SuperImpl.QUEUE_FULL);
            semaphore.releasePermit();
            throw new RejectedActionException(RejectionReason.QUEUE_FULL);
        }
    }

    private void handleRejectedReason(RejectionReason rejectionReason) {
        if (rejectionReason == RejectionReason.CIRCUIT_OPEN) {
            actionMetrics.incrementMetricCount(SuperImpl.CIRCUIT_OPEN);
        } else if (rejectionReason == RejectionReason.MAX_CONCURRENCY_LEVEL_EXCEEDED) {
            actionMetrics.incrementMetricCount(SuperImpl.MAX_CONCURRENCY_LEVEL_EXCEEDED);
        }
        throw new RejectedActionException(rejectionReason);
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        service.shutdown();
    }
}
