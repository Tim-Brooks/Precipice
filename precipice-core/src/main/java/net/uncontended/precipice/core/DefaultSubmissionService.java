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

package net.uncontended.precipice.core;

import net.uncontended.precipice.core.concurrent.DefaultResilientPromise;
import net.uncontended.precipice.core.concurrent.ResilientFuture;
import net.uncontended.precipice.core.concurrent.ResilientPromise;
import net.uncontended.precipice.core.concurrent.ResilientTask;
import net.uncontended.precipice.core.metrics.Metric;
import net.uncontended.precipice.core.timeout.TimeoutService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultSubmissionService extends AbstractService implements SubmissionService {
    private final ExecutorService service;
    private final TimeoutService timeoutService;

    public DefaultSubmissionService(ExecutorService service, ServiceProperties properties) {
        super(properties.circuitBreaker(), properties.actionMetrics(), properties.semaphore());
        this.timeoutService = properties.timeoutService();
        this.service = service;
    }

    public DefaultSubmissionService(ExecutorService service, ServiceProperties properties, AtomicBoolean isShutdown) {
        super(properties.circuitBreaker(), properties.actionMetrics(), properties.semaphore(), isShutdown);
        this.timeoutService = properties.timeoutService();
        this.service = service;
    }

    @Override
    public <T> ResilientFuture<T> submit(ResilientAction<T> action, long millisTimeout) {
        return submit(action, null, millisTimeout);
    }

    @Override
    public <T> ResilientFuture<T> submit(ResilientAction<T> action, ResilientCallback<T> callback, long millisTimeout) {
        acquirePermitOrRejectIfActionNotAllowed();
        ResilientPromise<T> promise = new DefaultResilientPromise<>();
        try {
            ResilientTask<T> task = new ResilientTask<>(actionMetrics, semaphore, circuitBreaker, action, callback,
                    promise, millisTimeout > MAX_TIMEOUT_MILLIS ? MAX_TIMEOUT_MILLIS : millisTimeout);
            service.execute(task);
            timeoutService.scheduleTimeout(task);

        } catch (RejectedExecutionException e) {
            actionMetrics.incrementMetricCount(Metric.QUEUE_FULL);
            semaphore.releasePermit();
            throw new RejectedActionException(RejectionReason.QUEUE_FULL);
        }
        return new ResilientFuture<>(promise);
    }

    @Override
    public void shutdown() {
        isShutdown.compareAndSet(false, true);
        service.shutdown();
    }
}
