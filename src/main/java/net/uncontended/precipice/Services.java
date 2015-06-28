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

import net.uncontended.precipice.circuit.CircuitBreaker;
import net.uncontended.precipice.circuit.NoOpCircuitBreaker;
import net.uncontended.precipice.concurrent.IntegerSemaphore;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.utils.PrecipiceExecutors;

import java.util.concurrent.ExecutorService;

public class Services {

    public static SubmissionService submissionService(String name, int poolSize, int concurrencyLevel) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultSubmissionService(executor, new IntegerSemaphore(concurrencyLevel));
    }

    public static SubmissionService submissionService(String name, int poolSize, int concurrencyLevel, ActionMetrics
            metrics) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultSubmissionService(executor, new IntegerSemaphore(concurrencyLevel), metrics);
    }

    public static SubmissionService submissionService(String name, int poolSize, int concurrencyLevel, CircuitBreaker
            breaker) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultSubmissionService(executor, new IntegerSemaphore(concurrencyLevel), breaker);
    }

    public static SubmissionService submissionService(String name, int poolSize, int concurrencyLevel, ActionMetrics
            metrics, CircuitBreaker breaker) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultSubmissionService(executor, new IntegerSemaphore(concurrencyLevel), metrics, breaker);
    }

    public static SubmissionService submissionService(ExecutorService executor, int concurrencyLevel, ActionMetrics
            metrics, CircuitBreaker breaker) {
        return new DefaultSubmissionService(executor, new IntegerSemaphore(concurrencyLevel), metrics, breaker);
    }

    public static SubmissionService submissionServiceWithNoOpBreaker(String name, int poolSize, int concurrencyLevel) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultSubmissionService(executor, new IntegerSemaphore(concurrencyLevel), new NoOpCircuitBreaker());
    }

    public static RunService runService(String name, int concurrencyLevel) {
        return new DefaultRunService(new IntegerSemaphore(concurrencyLevel));
    }

    public static RunService runService(String name, int concurrencyLevel, ActionMetrics metrics) {
        return new DefaultRunService(new IntegerSemaphore(concurrencyLevel), metrics);
    }

    public static RunService runService(String name, int concurrencyLevel, CircuitBreaker breaker) {
        return new DefaultRunService(new IntegerSemaphore(concurrencyLevel), breaker);
    }

    public static RunService runService(String name, int concurrencyLevel, ActionMetrics metrics,
                                        CircuitBreaker breaker) {
        return new DefaultRunService(new IntegerSemaphore(concurrencyLevel), metrics, breaker);
    }

    public static RunService runServiceWithNoOpBreaker(String name, int concurrencyLevel) {
        return new DefaultRunService(new IntegerSemaphore(concurrencyLevel), new NoOpCircuitBreaker());
    }

    public static CompletionService completionService(String name, int poolSize, int concurrencyLevel) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultCompletionService(executor, new IntegerSemaphore(concurrencyLevel));
    }

    public static CompletionService completionService(String name, int poolSize, int concurrencyLevel, ActionMetrics
            metrics) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultCompletionService(executor, new IntegerSemaphore(concurrencyLevel), metrics);
    }

    public static CompletionService completionService(String name, int poolSize, int concurrencyLevel, ActionMetrics
            metrics, CircuitBreaker breaker) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultCompletionService(executor, new IntegerSemaphore(concurrencyLevel), metrics, breaker);
    }

    public static CompletionService completionService(ExecutorService executor, int concurrencyLevel, ActionMetrics
            metrics, CircuitBreaker breaker) {
        return new DefaultCompletionService(executor, new IntegerSemaphore(concurrencyLevel), metrics, breaker);
    }

    public static CompletionService completionServiceWithNoOpBreaker(String name, int poolSize, int concurrencyLevel) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultCompletionService(executor, new IntegerSemaphore(concurrencyLevel), new NoOpCircuitBreaker());
    }

    public static MultiService defaultService(String name, int poolSize, int concurrencyLevel) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultService(executor, new IntegerSemaphore(concurrencyLevel));
    }

    public static MultiService defaultServiceWithNoOpBreaker(String name, int poolSize, int concurrencyLevel) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultService(executor, new IntegerSemaphore(concurrencyLevel), new NoOpCircuitBreaker());
    }

    public static MultiService defaultService(String name, int poolSize, int concurrencyLevel, ActionMetrics
            metrics) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultService(executor, new IntegerSemaphore(concurrencyLevel), metrics);
    }

    public static MultiService defaultService(String name, int poolSize, int concurrencyLevel, ActionMetrics
            metrics, CircuitBreaker breaker) {
        ExecutorService executor = PrecipiceExecutors.threadPoolExecutor(name, poolSize, concurrencyLevel);
        return new DefaultService(executor, new IntegerSemaphore(concurrencyLevel), metrics, breaker);
    }

    public static MultiService defaultService(ExecutorService executor, int concurrencyLevel, ActionMetrics
            metrics, CircuitBreaker breaker) {
        return new DefaultService(executor, new IntegerSemaphore(concurrencyLevel), metrics, breaker);
    }
}
