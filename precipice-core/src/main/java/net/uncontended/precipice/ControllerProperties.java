/*
 * Copyright 2016 Timothy Brooks
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
import net.uncontended.precipice.concurrent.IntegerSemaphore;
import net.uncontended.precipice.concurrent.PrecipiceSemaphore;
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.DefaultActionMetrics;
import net.uncontended.precipice.metrics.IntervalLatencyMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.timeout.TimeoutService;

public class ControllerProperties<T extends Enum<T> & Result> {


    private final Class<T> type;
    private ActionMetrics<T> metrics;
    private LatencyMetrics<T> latencyMetrics;
    private CircuitBreaker breaker = new DefaultCircuitBreaker(new BreakerConfigBuilder().build());
    private TimeoutService timeoutService = TimeoutService.defaultTimeoutService;
    private PrecipiceSemaphore semaphore;
    private int concurrencyLevel = Service.MAX_CONCURRENCY_LEVEL;

    public ControllerProperties(Class<T> type) {
        this.type = type;
        metrics = new DefaultActionMetrics<T>(type);
        latencyMetrics = new IntervalLatencyMetrics<>(type);
    }

    public ControllerProperties<T> actionMetrics(ActionMetrics<T> metrics) {
        this.metrics = metrics;
        return this;
    }

    public ActionMetrics<T> actionMetrics() {
        return metrics;
    }

    public ControllerProperties<T> circuitBreaker(CircuitBreaker breaker) {
        this.breaker = breaker;
        return this;
    }

    public CircuitBreaker circuitBreaker() {
        return breaker;
    }

    public ControllerProperties<T> timeoutService(TimeoutService timeoutService) {
        this.timeoutService = timeoutService;
        return this;
    }

    public TimeoutService timeoutService() {
        return timeoutService;
    }

    public ControllerProperties<T> concurrencyLevel(int concurrencyLevel) {
        this.concurrencyLevel = concurrencyLevel;
        return this;
    }

    public int concurrencyLevel() {
        return concurrencyLevel;
    }

    public ControllerProperties<T> semaphore(PrecipiceSemaphore semaphore) {
        this.semaphore = semaphore;
        return this;
    }

    public PrecipiceSemaphore semaphore() {
        // TODO: Consider whether this makes sense. It may not be clear.

        if (semaphore == null) {
            semaphore = new IntegerSemaphore(concurrencyLevel);
            return semaphore;
        }
        return semaphore;
    }

    public ControllerProperties<T> latencyMetrics(LatencyMetrics<T> latencyMetrics) {
        this.latencyMetrics = latencyMetrics;
        return this;
    }

    public LatencyMetrics<?> latencyMetrics() {
        return latencyMetrics;
    }
}
