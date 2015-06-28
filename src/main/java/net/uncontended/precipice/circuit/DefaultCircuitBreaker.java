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

package net.uncontended.precipice.circuit;

import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.HealthSnapshot;
import net.uncontended.precipice.utils.SystemTime;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by timbrooks on 11/5/14.
 */
public class DefaultCircuitBreaker implements CircuitBreaker {

    private static final int CLOSED = 0;
    private static final int OPEN = 1;
    private static final int FORCED_OPEN = 2;

    private final SystemTime systemTime;
    private final ActionMetrics actionMetrics;
    private final AtomicInteger state = new AtomicInteger(0);
    private final AtomicLong lastTestedTime = new AtomicLong(0);
    private final AtomicLong lastHealthTime = new AtomicLong(0);
    private final AtomicReference<HealthSnapshot> health = new AtomicReference<>();
    private final AtomicReference<BreakerConfig> breakerConfig;

    public DefaultCircuitBreaker(ActionMetrics actionMetrics, BreakerConfig breakerConfig) {
        this(actionMetrics, breakerConfig, new SystemTime());
    }

    public DefaultCircuitBreaker(ActionMetrics actionMetrics, BreakerConfig breakerConfig, SystemTime systemTime) {
        this.systemTime = systemTime;
        this.actionMetrics = actionMetrics;
        this.breakerConfig = new AtomicReference<>(breakerConfig);
    }

    @Override
    public boolean isOpen() {
        return state.get() != CLOSED;
    }

    @Override
    public boolean allowAction() {
        int state = this.state.get();
        if (state == OPEN) {
            long backOffTimeMillis = breakerConfig.get().backOffTimeMillis;
            long currentTime = systemTime.currentTimeMillis();
            // This potentially allows a couple of tests through. Should think about this decision
            if (currentTime < backOffTimeMillis + lastTestedTime.get()) {
                return false;
            }
            lastTestedTime.set(currentTime);
        }
        return state != FORCED_OPEN;
    }

    @Override
    public void informBreakerOfResult(boolean successful) {
        if (successful) {
            if (state.get() == OPEN) {
                // This can get stuck in a loop with open and closing
                state.compareAndSet(OPEN, CLOSED);
            }
        } else {
            if (state.get() == CLOSED) {
                long currentTime = systemTime.currentTimeMillis();
                BreakerConfig config = this.breakerConfig.get();
                HealthSnapshot health = getHealthSnapshot(config, currentTime);
                long failures = health.failures;
                double failurePercentage = health.failurePercentage();
                if (config.failureThreshold < failures || config.failurePercentageThreshold < failurePercentage) {
                    lastTestedTime.set(currentTime);
                    state.compareAndSet(CLOSED, OPEN);
                }
            }
        }
    }

    private HealthSnapshot getHealthSnapshot(BreakerConfig config, long currentTime) {
        long lastHealthTime = this.lastHealthTime.get();
        if (lastHealthTime + config.healthRefreshMillis < currentTime) {
            if (this.lastHealthTime.compareAndSet(lastHealthTime, currentTime)) {
                HealthSnapshot newHealth = actionMetrics.healthSnapshot(config.trailingPeriodMillis, TimeUnit.MILLISECONDS);
                health.set(newHealth);
                return newHealth;
            }
        }

        return health.get();
    }

    @Override
    public BreakerConfig getBreakerConfig() {
        return breakerConfig.get();
    }

    @Override
    public void setBreakerConfig(BreakerConfig breakerConfig) {
        this.breakerConfig.set(breakerConfig);
    }

    @Override
    public void forceOpen() {
        state.set(FORCED_OPEN);
    }

    @Override
    public void forceClosed() {
        state.set(CLOSED);
    }
}