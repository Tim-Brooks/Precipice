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
import net.uncontended.precipice.concurrent.PrecipiceSemaphore;
import net.uncontended.precipice.concurrent.UnlimitedSemaphore;
import net.uncontended.precipice.metrics.*;
import net.uncontended.precipice.metrics.CountMetrics;
import net.uncontended.precipice.metrics.DefaultCountMetrics;
import net.uncontended.precipice.time.Clock;
import net.uncontended.precipice.time.SystemTime;

public class ControllerProperties<T extends Enum<T> & Result> {

    public static int MAX_CONCURRENCY_LEVEL = Integer.MAX_VALUE / 2;


    private final Class<T> type;
    private CountMetrics<T> metrics;
    private LatencyMetrics<T> latencyMetrics;
    private CircuitBreaker breaker = new DefaultCircuitBreaker(new BreakerConfigBuilder().build());
    private PrecipiceSemaphore semaphore;
    private Clock clock = new SystemTime();

    public ControllerProperties(Class<T> type) {
        this.type = type;
        metrics = new DefaultCountMetrics<>(type);
        latencyMetrics = new IntervalLatencyMetrics<>(type);
        semaphore = new UnlimitedSemaphore();
    }

    public ControllerProperties<T> actionMetrics(CountMetrics<T> metrics) {
        this.metrics = metrics;
        return this;
    }

    public CountMetrics<T> actionMetrics() {
        return metrics;
    }

    public ControllerProperties<T> circuitBreaker(CircuitBreaker breaker) {
        this.breaker = breaker;
        return this;
    }

    public CircuitBreaker circuitBreaker() {
        return breaker;
    }

    public ControllerProperties<T> semaphore(PrecipiceSemaphore semaphore) {
        this.semaphore = semaphore;
        return this;
    }

    public PrecipiceSemaphore semaphore() {
        return semaphore;
    }

    public ControllerProperties<T> latencyMetrics(LatencyMetrics<T> latencyMetrics) {
        this.latencyMetrics = latencyMetrics;
        return this;
    }

    public LatencyMetrics<T> latencyMetrics() {
        return latencyMetrics;
    }

    public Clock clock() {
        return clock;
    }

    public void clock(Clock clock) {
        this.clock = clock;
    }
}
