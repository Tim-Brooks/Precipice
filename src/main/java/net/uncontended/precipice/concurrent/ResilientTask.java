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
import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.Metric;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

/**
 * Created by timbrooks on 6/12/15.
 */
public class ResilientTask<T> extends FutureTask<Void> {

    private final ResilientPromise<T> userPromise;
    private final ActionMetrics metrics;
    private final ExecutorSemaphore semaphore;
    private final CircuitBreaker breaker;
    private final ResilientCallback<T> callback;
    private final ResilientPromise<T> internalPromise;

    public ResilientTask(ActionMetrics metrics, ExecutorSemaphore semaphore, CircuitBreaker breaker, ResilientAction<T>
            action, ResilientCallback<T> callback, ResilientPromise<T> internalPromise, ResilientPromise<T> userPromise) {
        super(new ResilientCallable<>(action, internalPromise));
        this.metrics = metrics;
        this.semaphore = semaphore;
        this.breaker = breaker;
        this.callback = callback;
        this.internalPromise = internalPromise;
        this.userPromise = userPromise;
    }

    @Override
    protected void done() {
        metrics.incrementMetricCount(Metric.statusToMetric(internalPromise.getStatus()));
        breaker.informBreakerOfResult(internalPromise.isSuccessful());
        try {
            if (callback != null) {
                callback.run(userPromise == null ? internalPromise : userPromise);
            }
        } catch (Exception e) {
            // TODO: strategy for handling callback exception.

        } finally {
            semaphore.releasePermit();
        }
    }


    private static class ResilientCallable<T> implements Callable<Void> {
        private final ResilientPromise<T> promise;
        private final ResilientAction<T> action;

        public ResilientCallable(ResilientAction<T> action, ResilientPromise<T> promise) {
            this.action = action;
            this.promise = promise;
        }

        @Override
        public Void call() throws Exception {
            try {
                T result = action.run();
                promise.deliverResult(result);
            } catch (InterruptedException e) {
                Thread.interrupted();
            } catch (Exception e) {
                promise.deliverError(e);
            }
            return null;
        }
    }

}
