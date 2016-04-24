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

package net.uncontended.precipice.circuit;

import net.uncontended.precipice.Failable;
import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.metrics.NewMetrics;
import net.uncontended.precipice.metrics.PartitionedCount;
import net.uncontended.precipice.metrics.Rolling;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Unstable and still in development. At this time, {@link DefaultCircuitBreaker} should be used.
 */
public class NoOpenCircuit<Rejected extends Enum<Rejected>> implements CircuitBreaker<Rejected> {
    private static final int CLOSED = 0;
    private static final int OPEN = 1;
    private static final int FORCED_OPEN = 2;

    private final AtomicInteger state = new AtomicInteger(0);
    private final AtomicLong lastHealthTime = new AtomicLong(0);
    private final HealthGauge healthGauge;
    private final Runnable openRunnable;
    private volatile CircuitBreakerConfig<Rejected> breakerConfig;
    private volatile HealthSnapshot health = new HealthSnapshot(0, 0);

    public NoOpenCircuit(CircuitBreakerConfig<Rejected> breakerConfig, Runnable openRunnable) {
        this(breakerConfig, new HealthGauge(), openRunnable);
    }

    public NoOpenCircuit(CircuitBreakerConfig<Rejected> breakerConfig, HealthGauge healthGauge, Runnable openRunnable) {
        this.breakerConfig = breakerConfig;
        this.healthGauge = healthGauge;
        this.openRunnable = openRunnable;
    }

    @Override
    public boolean isOpen() {
        return state.get() != CLOSED;
    }

    @Override
    public CircuitBreakerConfig<Rejected> getBreakerConfig() {
        return breakerConfig;
    }

    @Override
    public void setBreakerConfig(CircuitBreakerConfig<Rejected> breakerConfig) {
        this.breakerConfig = breakerConfig;
    }

    @Override
    public void forceOpen() {
        state.set(FORCED_OPEN);
    }

    @Override
    public void forceClosed() {
        state.set(CLOSED);
    }

    @Override
    public Rejected acquirePermit(long number, long nanoTime) {
        CircuitBreakerConfig<Rejected> config = breakerConfig;
        int state = this.state.get();
        if (state == OPEN) {
            return config.reason;
        }
        return state != FORCED_OPEN ? null : config.reason;
    }

    @Override
    public void releasePermit(long number, long nanoTime) {
    }

    @Override
    public void releasePermit(long number, Failable result, long nanoTime) {
        if (state.get() == CLOSED) {
            long currentMillisTime = currentMillisTime(nanoTime);
            CircuitBreakerConfig<Rejected> config = breakerConfig;
            HealthSnapshot health = getHealthSnapshot(config, currentMillisTime, nanoTime);
            long failures = health.failures;
            int failurePercentage = health.failurePercentage();
            if (config.failureThreshold < failures || (config.failurePercentageThreshold < failurePercentage &&
                    config.sampleSizeThreshold < health.total)) {
                if (state.compareAndSet(CLOSED, OPEN)) {
                    // TODO: Obviously need some timing mechanism to ensure there are not multiple runs
                    openRunnable.run();
                }
            }
        }
    }

    @Override
    public <Result extends Enum<Result> & Failable> void registerGuardRail(GuardRail<Result, Rejected> guardRail) {
        NewMetrics<PartitionedCount<Result>> metrics = guardRail.getResultMetrics();
        if (metrics instanceof Rolling) {
            healthGauge.add((Rolling<PartitionedCount<Result>>) metrics);
        } else {
            throw new IllegalArgumentException("NoOpenCircuit requires rolling result object");
        }
    }

    private HealthSnapshot getHealthSnapshot(CircuitBreakerConfig<Rejected> config, long currentMillisTime, long nanoTime) {
        long lastHealthTime = this.lastHealthTime.get();
        // TODO: Flawed way to compare time? I think it is correct, but check for negative case
        if (lastHealthTime + config.healthRefreshMillis < currentMillisTime) {
            if (this.lastHealthTime.compareAndSet(lastHealthTime, currentMillisTime)) {
                HealthSnapshot newHealth = healthGauge.getHealth(config.trailingPeriodMillis, TimeUnit.MILLISECONDS, nanoTime);
                health = newHealth;
                return newHealth;
            }
        }
        return health;
    }

    private static long currentMillisTime(long nanoTime) {
        return TimeUnit.MILLISECONDS.convert(nanoTime, TimeUnit.NANOSECONDS);
    }
}
