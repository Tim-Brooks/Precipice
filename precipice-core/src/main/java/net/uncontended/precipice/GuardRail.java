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

import net.uncontended.precipice.metrics.CountMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.time.Clock;

import java.util.List;

public class GuardRail<Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>> {

    private final CountMetrics<Result> resultMetrics;
    private final CountMetrics<Rejected> rejectedMetrics;
    private final LatencyMetrics<Result> latencyMetrics;
    private final String name;
    private final Clock clock;
    private final PrecipiceFunction<Result, ExecutionContext> releaseFunction;
    private List<BackPressure<Rejected>> backPressureList;

    private GuardRail(GuardRailProperties<Result, Rejected> properties) {
        this.resultMetrics = properties.resultMetrics;
        this.rejectedMetrics = properties.rejectedMetrics;
        this.latencyMetrics = properties.resultLatency;
        this.name = properties.name;
        this.clock = properties.clock;
        this.backPressureList = properties.backPressureList;
        this.releaseFunction = new FinishingCallback();
    }

    public Rejected acquirePermits(long number) {
        return acquirePermits(number, clock.nanoTime());
    }

    public Rejected acquirePermits(long number, long nanoTime) {
        for (int i = 0; i < backPressureList.size(); ++i) {
            BackPressure<Rejected> bp = backPressureList.get(i);
            Rejected rejected = bp.acquirePermit(number, nanoTime);
            if (rejected != null) {
                rejectedMetrics.incrementMetricCount(rejected, nanoTime);
                for (int j = 0; j < i; ++j) {
                    backPressureList.get(j).releasePermit(number, nanoTime);
                }
                return rejected;
            }
        }
        return null;
    }

    public void releasePermitsWithoutResult(long number) {
        releasePermitsWithoutResult(number, clock.nanoTime());
    }

    public void releasePermitsWithoutResult(long number, long nanoTime) {
        for (BackPressure backPressure : backPressureList) {
            backPressure.releasePermit(number, nanoTime);
        }
    }

    public void releasePermits(ExecutionContext context, Result result) {
        releasePermits(context.permitCount(), result, context.startNanos(), clock.nanoTime());
    }

    public void releasePermits(ExecutionContext context, Result result, long nanoTime) {
        releasePermits(context.permitCount(), result, context.startNanos(), nanoTime);
    }

    public void releasePermits(long number, Result result, long startNanos) {
        releasePermits(number, result, startNanos, clock.nanoTime());
    }

    public void releasePermits(long number, Result result, long startNanos, long nanoTime) {
        resultMetrics.incrementMetricCount(result, nanoTime);
        latencyMetrics.recordLatency(result, nanoTime - startNanos, nanoTime);
        for (BackPressure backPressure : backPressureList) {
            backPressure.releasePermit(number, result, nanoTime);
        }
    }

    public PrecipiceFunction<Result, ExecutionContext> releaseFunction() {
        return releaseFunction;
    }

    public String getName() {
        return name;
    }

    public CountMetrics<Result> getResultMetrics() {
        return resultMetrics;
    }

    public CountMetrics<Rejected> getRejectedMetrics() {
        return rejectedMetrics;
    }

    public LatencyMetrics<Result> getLatencyMetrics() {
        return latencyMetrics;
    }

    public Clock getClock() {
        return clock;
    }

    public static <Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>> GuardRail<Result, Rejected>
    create(GuardRailProperties<Result, Rejected> properties) {
        GuardRail<Result, Rejected> guardRail = new GuardRail<>(properties);
        guardRail.wireUp();
        return guardRail;
    }

    private void wireUp() {
        for (BackPressure<Rejected> bp : backPressureList) {
            bp.registerGuardRail(this);
        }
    }

    private class FinishingCallback implements PrecipiceFunction<Result, ExecutionContext> {

        @Override
        public void apply(Result result, ExecutionContext context) {
            long endTime = clock.nanoTime();
            releasePermits(context, result, endTime);
        }
    }
}
