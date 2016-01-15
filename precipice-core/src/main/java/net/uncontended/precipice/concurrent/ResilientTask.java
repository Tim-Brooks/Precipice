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

import net.uncontended.precipice.NewController;
import net.uncontended.precipice.ResilientAction;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.timeout.ActionTimeoutException;
import net.uncontended.precipice.timeout.TimeoutService;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ResilientTask<T> implements Runnable, Delayed {

    public final AtomicReference<Status> status = new AtomicReference<>(null);
    public final long nanosAbsoluteTimeout;
    public final long nanosAbsoluteStart;
    public final long millisRelativeTimeout;
    private final PrecipicePromise<Status, T> promise;
    private final LatencyMetrics<Status> latencyMetrics;
    private final ActionMetrics<Status> metrics;
    private final PrecipiceSemaphore semaphore;
    private final CircuitBreaker breaker;
    private final ResilientAction<T> action;
    private volatile Thread runner;

    public ResilientTask(NewController<Status> controller, ResilientAction<T> action, PrecipicePromise<Status, T> promise,
                         long millisRelativeTimeout, long nanosAbsoluteStart) {
        metrics = controller.getActionMetrics();
        latencyMetrics = controller.getLatencyMetrics();
        semaphore = controller.getSemaphore();
        breaker = controller.getCircuitBreaker();
        this.action = action;
        this.promise = promise;
        this.millisRelativeTimeout = millisRelativeTimeout;
        this.nanosAbsoluteStart = nanosAbsoluteStart;
        if (millisRelativeTimeout == TimeoutService.NO_TIMEOUT) {
            nanosAbsoluteTimeout = 0;
        } else {
            nanosAbsoluteTimeout = TimeUnit.NANOSECONDS.convert(millisRelativeTimeout, TimeUnit.MILLISECONDS)
                    + nanosAbsoluteStart;
        }
    }

    @Override
    public void run() {
        try {
            if (status.get() == null) {
                runner = Thread.currentThread();
                T result = action.run();
                if (status.compareAndSet(null, Status.SUCCESS)) {
                    safeSetSuccess(result);
                }
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
        } catch (ActionTimeoutException e) {
            if (status.compareAndSet(null, Status.TIMEOUT)) {
                safeSetTimedOut(e);
            }
        } catch (Throwable e) {
            if (status.compareAndSet(null, Status.ERROR)) {
                safeSetErred(e);
            }
        } finally {
            done();
        }
    }

    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(nanosAbsoluteTimeout - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (o instanceof ResilientTask) {
            return Long.compare(nanosAbsoluteTimeout, ((ResilientTask<T>) o).nanosAbsoluteTimeout);
        }
        return Long.compare(getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));
    }

    public void setTimedOut() {
        if (status.get() == null) {
            if (status.compareAndSet(null, Status.TIMEOUT)) {
                safeSetTimedOut(new ActionTimeoutException());
                if (runner != null) {
                    runner.interrupt();
                }
            }
        }
    }

    private void safeSetSuccess(T result) {
        try {
            promise.complete(Status.SUCCESS, result);
        } catch (Throwable t) {
        }
    }

    private void safeSetErred(Throwable e) {
        try {
            promise.completeExceptionally(Status.ERROR, e);
        } catch (Throwable t) {
        }
    }

    private void safeSetTimedOut(ActionTimeoutException e) {
        try {
            promise.completeExceptionally(Status.TIMEOUT, e);
        } catch (Throwable t) {
        }
    }

    private void done() {
        long nanoTime = System.nanoTime();
        Status status = this.status.get();
        metrics.incrementMetricCount(status, nanoTime);
        breaker.informBreakerOfResult(status.isSuccess(), nanoTime);
        if (status.trackLatency()) {
            long latency = nanoTime - nanosAbsoluteStart;
            latencyMetrics.recordLatency(status, latency, nanoTime);
        }
        semaphore.releasePermit();
    }

}
