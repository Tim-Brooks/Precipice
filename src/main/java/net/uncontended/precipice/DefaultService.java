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
import net.uncontended.precipice.concurrent.PrecipiceSemaphore;
import net.uncontended.precipice.concurrent.ResilientFuture;
import net.uncontended.precipice.concurrent.ResilientPromise;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.DefaultActionMetrics;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultService extends AbstractService implements MultiService {

    private final ExecutorService service;
    private final RunService runService;
    private final DefaultSubmissionService submissionService;
    private final DefaultCompletionService completionService;

    public DefaultService(ExecutorService service, PrecipiceSemaphore semaphore) {
        this(service, semaphore, new DefaultActionMetrics());
    }

    public DefaultService(ExecutorService service, PrecipiceSemaphore semaphore, ActionMetrics actionMetrics) {
        this(service, semaphore, actionMetrics, new DefaultCircuitBreaker(new BreakerConfigBuilder().build()));
    }

    public DefaultService(ExecutorService service, PrecipiceSemaphore semaphore, CircuitBreaker breaker) {
        this(service, semaphore, new DefaultActionMetrics(), breaker);
    }

    public DefaultService(ExecutorService service, PrecipiceSemaphore semaphore, ActionMetrics actionMetrics, CircuitBreaker
            circuitBreaker) {
        this(service, semaphore, actionMetrics, circuitBreaker, new AtomicBoolean(false));
    }

    private DefaultService(ExecutorService service, PrecipiceSemaphore semaphore, ActionMetrics actionMetrics,
                           CircuitBreaker circuitBreaker, AtomicBoolean isShutdown) {
        super(circuitBreaker, actionMetrics, semaphore, isShutdown);
        this.service = service;
        this.runService = new DefaultRunService(semaphore, actionMetrics, circuitBreaker, isShutdown);
        this.submissionService = new DefaultSubmissionService(service, semaphore, actionMetrics, circuitBreaker, isShutdown);
        this.completionService = new DefaultCompletionService(service, semaphore, actionMetrics, circuitBreaker, isShutdown);
    }

    @Override
    public <T> ResilientFuture<T> submit(ResilientAction<T> action, long millisTimeout) {
        return submissionService.submit(action, millisTimeout);
    }

    @Override
    public <T> void submitAndComplete(ResilientAction<T> action, ResilientPromise<T> promise, long millisTimeout) {
        completionService.submitAndComplete(action, promise, null, millisTimeout);
    }

    @Override
    public <T> ResilientFuture<T> submit(ResilientAction<T> action, ResilientCallback<T> callback, long millisTimeout) {
        return submissionService.submit(action, callback, millisTimeout);
    }

    @Override
    public <T> void submitAndComplete(ResilientAction<T> action, ResilientPromise<T> promise,
                                      ResilientCallback<T> callback, long millisTimeout) {
        completionService.submitAndComplete(action, promise, callback, millisTimeout);
    }

    @Override
    public <T> T run(ResilientAction<T> action) throws Exception {
        try {
            return runService.run(action);
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
