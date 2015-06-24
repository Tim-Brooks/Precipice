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
import net.uncontended.precipice.ResilientCallback;
import net.uncontended.precipice.Status;
import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.Metric;

import java.util.concurrent.atomic.AtomicReference;

public class ResilientTask<T> implements Runnable {

    public final AtomicReference<Status> status = new AtomicReference<>(Status.PENDING);
    private final ResilientPromise<T> promise;
    private final ActionMetrics metrics;
    private final PrecipiceSemaphore semaphore;
    private final CircuitBreaker breaker;
    private final ResilientAction<T> action;
    private final ResilientCallback<T> callback;
    private volatile Thread runner;

    public ResilientTask(ActionMetrics metrics, PrecipiceSemaphore semaphore, CircuitBreaker breaker, ResilientAction<T>
            action, ResilientCallback<T> callback, ResilientPromise<T> promise) {
        this.metrics = metrics;
        this.semaphore = semaphore;
        this.breaker = breaker;
        this.action = action;
        this.callback = callback;
        this.promise = promise;
    }

    @Override
    public void run() {
        try {
            if (status.get() == Status.PENDING) {
                runner = Thread.currentThread();
                T result = action.run();
                if (status.compareAndSet(Status.PENDING, Status.SUCCESS)) {
                    promise.deliverResult(result);
                }
            }
        } catch (InterruptedException e) {
            Thread.interrupted();
        } catch (Exception e) {
            if (status.compareAndSet(Status.PENDING, Status.ERROR)) {
                promise.deliverError(e);
            }
        } finally {
            done();
        }
    }

    public void setTimedOut() {
        if (status.get() == Status.PENDING) {
            if (status.compareAndSet(Status.PENDING, Status.TIMEOUT)) {
                if (runner != null) {
                    runner.interrupt();
                }
            }
        }
    }

    private void done() {
        metrics.incrementMetricCount(Metric.statusToMetric(status.get()));
        breaker.informBreakerOfResult(status.get() == Status.SUCCESS);
        try {
            if (callback != null) {
                callback.run(promise);
            }
        } catch (Exception e) {
            // TODO: strategy for handling callback exception.
        } finally {
            semaphore.releasePermit();
        }
    }

}
