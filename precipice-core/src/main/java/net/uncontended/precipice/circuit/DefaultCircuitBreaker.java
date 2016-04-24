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
import net.uncontended.precipice.metrics.*;
import net.uncontended.precipice.metrics.counts.WritableCounts;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultCircuitBreaker<Rejected extends Enum<Rejected>> implements CircuitBreaker<Rejected> {
    private static final int CLOSED = 0;
    private static final int OPEN = 1;
    private static final int FORCED_OPEN = 2;

    private final AtomicInteger state = new AtomicInteger(0);
    private final AtomicLong lastHealthTime = new AtomicLong(0);
    private final HealthGauge healthGauge;
    private volatile long lastTestedTime = 0;
    private volatile CircuitBreakerConfig<Rejected> breakerConfig;
    private volatile HealthSnapshot health = new HealthSnapshot(0, 0);

    public DefaultCircuitBreaker(CircuitBreakerConfig<Rejected> breakerConfig) {
        this(breakerConfig, new HealthGauge());
    }

    public DefaultCircuitBreaker(CircuitBreakerConfig<Rejected> breakerConfig, HealthGauge healthGauge) {
        this.breakerConfig = breakerConfig;
        this.healthGauge = healthGauge;
    }

    @Override
    public Rejected acquirePermit(long number, long nanoTime) {
        CircuitBreakerConfig<Rejected> config = breakerConfig;
        int state = this.state.get();
        if (state == OPEN) {
            long backOffTimeMillis = config.backOffTimeMillis;
            long currentTime = currentMillisTime(nanoTime);
            // This potentially allows a couple of tests through. Should think about this decision
            if (currentTime < backOffTimeMillis + lastTestedTime) {
                return config.reason;
            }
            lastTestedTime = currentTime;
        }
        return state != FORCED_OPEN ? null : config.reason;
    }

    @Override
    public void releasePermit(long number, long nanoTime) {
    }

    @Override
    public void releasePermit(long number, Failable result, long nanoTime) {
        if (result.isSuccess()) {
            if (state.get() == OPEN) {
                // Explore whether this can get stuck in a loop with open and closing
                state.compareAndSet(OPEN, CLOSED);
            }
        } else {
            if (state.get() == CLOSED) {
                long currentTime = currentMillisTime(nanoTime);
                CircuitBreakerConfig<Rejected> config = breakerConfig;
                HealthSnapshot health = getHealthSnapshot(config, nanoTime);
                long failures = health.failures;
                int failurePercentage = health.failurePercentage();
                if (config.failureThreshold < failures || (config.failurePercentageThreshold < failurePercentage &&
                        config.sampleSizeThreshold < health.total)) {
                    lastTestedTime = currentTime;
                    state.compareAndSet(CLOSED, OPEN);
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
            throw new IllegalArgumentException("DefaultCircuitBreaker requires rolling result object");
        }
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

    private HealthSnapshot getHealthSnapshot(CircuitBreakerConfig<Rejected> config, long currentTime) {
        long lastHealthTime = this.lastHealthTime.get();
        if (lastHealthTime + config.healthRefreshMillis < currentTime) {
            if (this.lastHealthTime.compareAndSet(lastHealthTime, currentTime)) {
                HealthSnapshot newHealth = healthGauge.getHealth(config.trailingPeriodMillis, TimeUnit.MILLISECONDS,
                        currentTime);
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
