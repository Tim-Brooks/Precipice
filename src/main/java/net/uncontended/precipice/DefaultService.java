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

public class DefaultService extends AbstractService implements MultiService {

    private final ExecutorService service;
    private final TimeoutService timeoutService = TimeoutService.defaultTimeoutService;

    public DefaultService(ExecutorService service, PrecipiceSemaphore semaphore) {
        this(service, semaphore, new DefaultActionMetrics());
    }

    public DefaultService(ExecutorService service, PrecipiceSemaphore semaphore, ActionMetrics actionMetrics) {
        this(service, semaphore, actionMetrics, new DefaultCircuitBreaker(actionMetrics, new
                BreakerConfigBuilder().build()));
    }

    public DefaultService(ExecutorService service, PrecipiceSemaphore semaphore, CircuitBreaker breaker) {
        this(service, semaphore, new DefaultActionMetrics(), breaker);
    }

    public DefaultService(ExecutorService service, PrecipiceSemaphore semaphore, ActionMetrics actionMetrics, CircuitBreaker
            circuitBreaker) {
        super(circuitBreaker, actionMetrics, semaphore);
        this.service = service;
    }

    @Override
    public <T> ResilientFuture<T> submit(ResilientAction<T> action, long millisTimeout) {
        return submit(action, null, millisTimeout);
    }

    @Override
    public <T> void submitAndComplete(ResilientAction<T> action, ResilientPromise<T> promise, long
            millisTimeout) {
        submitAndComplete(action, promise, null, millisTimeout);
    }

    @Override
    public <T> ResilientFuture<T> submit(ResilientAction<T> action, ResilientCallback<T> callback, long
            millisTimeout) {
        ResilientPromise<T> promise = new DefaultResilientPromise<>();
        submitAndComplete(action, promise, callback, millisTimeout);
        return new ResilientFuture<>(promise);
    }

    @Override
    public <T> void submitAndComplete(final ResilientAction<T> action, final ResilientPromise<T> promise,
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
    public <T> T run(final ResilientAction<T> action) throws Exception {
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
}
