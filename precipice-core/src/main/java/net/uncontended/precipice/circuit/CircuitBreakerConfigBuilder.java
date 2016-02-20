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

public class CircuitBreakerConfigBuilder<Rejected extends Enum<Rejected>> {
    private final Rejected reason;
    private final Rejected forcedReason;
    public long trailingPeriodMillis = 5000;
    public long failureThreshold = Long.MAX_VALUE;
    public int failurePercentageThreshold = 50;
    public long healthRefreshMillis = 500;
    public long backOffTimeMillis = 1000;
    public long sampleSizeThreshold = 10;

    public CircuitBreakerConfigBuilder(Rejected reason) {
        this(reason, reason);
    }

    public CircuitBreakerConfigBuilder(Rejected reason, Rejected forcedReason) {
        this.reason = reason;
        this.forcedReason = forcedReason;
    }

    public CircuitBreakerConfigBuilder<Rejected> trailingPeriodMillis(long trailingPeriodMillis) {
        this.trailingPeriodMillis = trailingPeriodMillis;
        return this;
    }

    public CircuitBreakerConfigBuilder<Rejected> failureThreshold(long failureThreshold) {
        this.failureThreshold = failureThreshold;
        return this;
    }

    public CircuitBreakerConfigBuilder<Rejected> failurePercentageThreshold(int failurePercentageThreshold) {
        this.failurePercentageThreshold = failurePercentageThreshold;
        return this;
    }

    public CircuitBreakerConfigBuilder<Rejected> backOffTimeMillis(long backOffTimeMillis) {
        this.backOffTimeMillis = backOffTimeMillis;
        return this;
    }

    public CircuitBreakerConfigBuilder<Rejected> healthRefreshMillis(long healthRefreshMillis) {
        this.healthRefreshMillis = healthRefreshMillis;
        return this;
    }

    public CircuitBreakerConfigBuilder<Rejected> sampleSizeThreshold(long sampleSizeThreshold) {
        this.sampleSizeThreshold = sampleSizeThreshold;
        return this;
    }

    public CircuitBreakerConfig<Rejected> build() {
        return new CircuitBreakerConfig<>(reason, forcedReason, failureThreshold, failurePercentageThreshold,
                trailingPeriodMillis, healthRefreshMillis, backOffTimeMillis, sampleSizeThreshold);
    }

}
