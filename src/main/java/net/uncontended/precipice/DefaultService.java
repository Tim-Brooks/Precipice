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

import net.uncontended.precipice.circuit.BreakerConfigBuilder;
import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.circuit.DefaultCircuitBreaker;
import net.uncontended.precipice.concurrent.*;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.DefaultActionMetrics;
import net.uncontended.precipice.metrics.Metric;
import net.uncontended.precipice.timeout.ActionTimeout;
import net.uncontended.precipice.timeout.TimeoutService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultService extends AbstractService implements MultiService {

    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    private final ExecutorService service;
    private final TimeoutService timeoutService = TimeoutService.defaultTimeoutService;
    private final ExecutorSemaphore semaphore;


    public DefaultService(ExecutorService service, int concurrencyLevel) {
        this(service, concurrencyLevel, new DefaultActionMetrics());
    }

    public DefaultService(ExecutorService service, int concurrencyLevel, ActionMetrics actionMetrics) {
        this(service, concurrencyLevel, actionMetrics, new DefaultCircuitBreaker(actionMetrics, new
                BreakerConfigBuilder().build()));
    }

    public DefaultService(ExecutorService service, int concurrencyLevel, CircuitBreaker breaker) {
        this(service, concurrencyLevel, new DefaultActionMetrics(), breaker);
    }

    public DefaultService(ExecutorService service, int concurrencyLevel, ActionMetrics actionMetrics, CircuitBreaker
            circuitBreaker) {
        super(circuitBreaker, actionMetrics);
        this.semaphore = new ExecutorSemaphore(concurrencyLevel);
        this.service = service;
    }

    @Override
    public <T> ResilientFuture<T> submitAction(ResilientAction<T> action, long millisTimeout) {
        return submitAction(action, (ResilientCallback<T>) null, millisTimeout);
    }

    @Override
    public <T> void submitAction(ResilientAction<T> action, ResilientPromise<T> promise, long
            millisTimeout) {
        submitAction(action, promise, null, millisTimeout);
    }

    @Override
    public <T> ResilientFuture<T> submitAction(ResilientAction<T> action, ResilientCallback<T> callback, long
            millisTimeout) {
        ResilientPromise<T> promise = new DefaultResilientPromise<>();
        submitAction(action, promise, callback, millisTimeout);
        return new ResilientFuture<>(promise);
    }

    @Override
    public <T> void submitAction(final ResilientAction<T> action, final ResilientPromise<T> promise,
                                 final ResilientCallback<T> callback, long millisTimeout) {
        acquirePermitOrRejectIfActionNotAllowed();
        try {
            ResilientTask<T> task = new ResilientTask<>(actionMetrics, semaphore, circuitBreaker, action, callback,
                    promise);
            service.execute(task);

            if (millisTimeout > MAX_TIMEOUT_MILLIS) {
                timeoutService.scheduleTimeout(new ActionTimeout(MAX_TIMEOUT_MILLIS, promise, task));
            } else {
                timeoutService.scheduleTimeout(new ActionTimeout(millisTimeout, promise, task));
            }
        } catch (RejectedExecutionException e) {
            actionMetrics.incrementMetricCount(Metric.QUEUE_FULL);
            semaphore.releasePermit();
            throw new RejectedActionException(RejectionReason.QUEUE_FULL);
        }
    }

    @Override
    public <T> T performAction(final ResilientAction<T> action) throws Exception {
        acquirePermitOrRejectIfActionNotAllowed();
        try {
            T result = action.run();
            actionMetrics.incrementMetricCount(Metric.statusToMetric(Status.SUCCESS));
            return result;
        } catch (ActionTimeoutException e) {
            actionMetrics.incrementMetricCount(Metric.statusToMetric(Status.TIMEOUT));
            throw e;
        } catch (Exception e) {
            actionMetrics.incrementMetricCount(Metric.statusToMetric(Status.ERROR));
            throw e;
        } finally {
            semaphore.releasePermit();
        }
    }

    @Override
    public void shutdown() {
        isShutdown.compareAndSet(false, true);
        service.shutdown();
    }

    private void acquirePermitOrRejectIfActionNotAllowed() {
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
