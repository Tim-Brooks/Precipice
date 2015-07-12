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


import net.uncontended.precipice.core.concurrent.Promise;
import net.uncontended.precipice.core.concurrent.ResilientPromise;
import net.uncontended.precipice.core.concurrent.ResilientTask;
import net.uncontended.precipice.core.metrics.Metric;
import net.uncontended.precipice.core.timeout.TimeoutService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultCompletionService extends AbstractService implements CompletionService {
    private final ExecutorService service;
    private final TimeoutService timeoutService;

    public DefaultCompletionService(String name, ExecutorService service, ServiceProperties properties) {
        super(name, properties.circuitBreaker(), properties.actionMetrics(), properties.semaphore());
        this.timeoutService = properties.timeoutService();
        this.service = service;
    }

    public DefaultCompletionService(String name, ExecutorService service, ServiceProperties properties,
                                    AtomicBoolean isShutdown) {
        super(name, properties.circuitBreaker(), properties.actionMetrics(), properties.semaphore(), isShutdown);
        this.timeoutService = properties.timeoutService();
        this.service = service;
    }

    @Override
    public <T> void submitAndComplete(ResilientAction<T> action, Promise<T> promise, long millisTimeout) {
        acquirePermitOrRejectIfActionNotAllowed();
        try {
            ResilientTask<T> task = new ResilientTask<>(actionMetrics, semaphore, circuitBreaker, action,
                    promise, millisTimeout > MAX_TIMEOUT_MILLIS ? MAX_TIMEOUT_MILLIS : millisTimeout);
            service.execute(task);
            timeoutService.scheduleTimeout(task);

        } catch (RejectedExecutionException e) {
            actionMetrics.incrementMetricCount(Metric.QUEUE_FULL);
            semaphore.releasePermit();
            throw new RejectedActionException(RejectionReason.QUEUE_FULL);
        }
    }

    @Override
    public void shutdown() {
        isShutdown.compareAndSet(false, true);
        service.shutdown();
    }
}
