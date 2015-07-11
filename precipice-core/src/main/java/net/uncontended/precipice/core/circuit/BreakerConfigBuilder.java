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

package net.uncontended.precipice.core.circuit;

public class BreakerConfigBuilder {
    public long trailingPeriodMillis = 1000;
    public long failureThreshold = Long.MAX_VALUE;
    public int failurePercentageThreshold = 50;
    public long healthRefreshMillis = 500;
    public long backOffTimeMillis = 1000;

    public BreakerConfigBuilder trailingPeriodMillis(long trailingPeriodMillis) {
        this.trailingPeriodMillis = trailingPeriodMillis;
        return this;
    }

    public BreakerConfigBuilder failureThreshold(long failureThreshold) {
        this.failureThreshold = failureThreshold;
        return this;
    }

    public BreakerConfigBuilder failurePercentageThreshold(int failurePercentageThreshold) {
        this.failurePercentageThreshold = failurePercentageThreshold;
        return this;
    }

    public BreakerConfigBuilder backOffTimeMillis(long backOffTimeMillis) {
        this.backOffTimeMillis = backOffTimeMillis;
        return this;
    }

    public BreakerConfigBuilder healthRefreshMillis(long healthRefreshMillis) {
        this.healthRefreshMillis = healthRefreshMillis;
        return this;
    }

    public BreakerConfig build() {
        return new BreakerConfig(failureThreshold, failurePercentageThreshold, trailingPeriodMillis,
                healthRefreshMillis, backOffTimeMillis);
    }

}
