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

package net.uncontended.precipice.concurrent;

import net.uncontended.precipice.ResilientAction;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.Metric;
import net.uncontended.precipice.timeout.ActionTimeoutException;
import net.uncontended.precipice.timeout.TimeoutService;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ResilientTask<T> implements Runnable, Delayed {

    public final AtomicReference<Status> status = new AtomicReference<>(Status.PENDING);
    public final long millisAbsoluteTimeout;
    public final long millisRelativeTimeout;
    private final PrecipicePromise<T> promise;
    private final ActionMetrics metrics;
    private final PrecipiceSemaphore semaphore;
    private final CircuitBreaker breaker;
    private final ResilientAction<T> action;
    private volatile Thread runner;

    public ResilientTask(ActionMetrics metrics, PrecipiceSemaphore semaphore, CircuitBreaker breaker, ResilientAction<T>
            action, PrecipicePromise<T> promise, long millisRelativeTimeout) {
        this.metrics = metrics;
        this.semaphore = semaphore;
        this.breaker = breaker;
        this.action = action;
        this.promise = promise;
        this.millisRelativeTimeout = millisRelativeTimeout;
        if (millisRelativeTimeout == TimeoutService.NO_TIMEOUT) {
            this.millisAbsoluteTimeout = 0;
        } else {
            this.millisAbsoluteTimeout = millisRelativeTimeout + System.currentTimeMillis();
        }
    }

    @Override
    public void run() {
        try {
            if (status.get() == Status.PENDING) {
                runner = Thread.currentThread();
                T result = action.run();
                if (status.compareAndSet(Status.PENDING, Status.SUCCESS)) {
                    safeSetSuccess(result);
                }
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
        } catch (ActionTimeoutException e) {
            if (status.compareAndSet(Status.PENDING, Status.TIMEOUT)) {
                safeSetTimedOut();
            }
        } catch (Throwable e) {
            if (status.compareAndSet(Status.PENDING, Status.ERROR)) {
                safeSetErred(e);
            }
        } finally {
            done();
        }
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(millisAbsoluteTimeout - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (o instanceof ResilientTask) {
            return Long.compare(millisAbsoluteTimeout, ((ResilientTask) o).millisAbsoluteTimeout);
        }
        return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
    }

    public void setTimedOut() {
        if (status.get() == Status.PENDING) {
            if (status.compareAndSet(Status.PENDING, Status.TIMEOUT)) {
                safeSetTimedOut();
                if (runner != null) {
                    runner.interrupt();
                }
            }
        }
    }

    private void safeSetSuccess(T result) {
        try {
            promise.complete(result);
        } catch (Throwable t) {}
    }

    private void safeSetErred(Throwable e) {
        try {
            promise.completeExceptionally(e);
        } catch (Throwable t) {}
    }

    private void safeSetTimedOut() {
        try {
            promise.completeWithTimeout();
        } catch (Throwable t) {}
    }

    private void done() {
        metrics.incrementMetricCount(Metric.statusToMetric(status.get()));
        breaker.informBreakerOfResult(status.get() == Status.SUCCESS);
        semaphore.releasePermit();
    }

}
