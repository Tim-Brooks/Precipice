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

import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.metrics.TotalCountMetrics;
import net.uncontended.precipice.time.Clock;

import java.util.List;

public class GuardRail<Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>> {

    private final TotalCountMetrics<Result> resultMetrics;
    private final TotalCountMetrics<Rejected> rejectedMetrics;
    private final LatencyMetrics<Result> latencyMetrics;
    private final String name;
    private final Clock clock;
    private final PrecipiceFunction<Result, PerformingContext> releaseFunction;
    private volatile boolean isShutdown = false;
    private List<BackPressure<Rejected>> backPressureList;

    public GuardRail(String name, TotalCountMetrics<Result> resultMetrics, TotalCountMetrics<Rejected> rejectedMetrics,
                     LatencyMetrics<Result> latencyMetrics, List<BackPressure<Rejected>> backPressureList, Clock clock) {
        this.resultMetrics = resultMetrics;
        this.rejectedMetrics = rejectedMetrics;
        this.latencyMetrics = latencyMetrics;
        this.name = name;
        this.clock = clock;
        this.backPressureList = backPressureList;
        this.releaseFunction = new FinishingCallback();

        for (BackPressure<Rejected> bp : backPressureList) {
            bp.registerResultMetrics(resultMetrics);
        }
    }

    public Rejected acquirePermits(long number) {
        return acquirePermits(number, clock.nanoTime());
    }

    public Rejected acquirePermits(long number, long nanoTime) {
        if (isShutdown) {
            throw new IllegalStateException("Service has been shutdown.");
        }

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

    public void releasePermits(PerformingContext context, Result result) {
        releasePermits(context.permitCount(), result, context.startNanos(), clock.nanoTime());
    }

    public void releasePermits(PerformingContext context, Result result, long nanoTime) {
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

    public PrecipiceFunction<Result, PerformingContext> releaseFunction() {
        return releaseFunction;
    }

    public void shutdown() {
        isShutdown = true;
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public String getName() {
        return name;
    }

    public TotalCountMetrics<Result> getResultMetrics() {
        return resultMetrics;
    }

    public TotalCountMetrics<Rejected> getRejectedMetrics() {
        return rejectedMetrics;
    }

    public LatencyMetrics<Result> getLatencyMetrics() {
        return latencyMetrics;
    }

    public Clock getClock() {
        return clock;
    }

    private class FinishingCallback implements PrecipiceFunction<Result, PerformingContext> {

        @Override
        public void apply(Result result, PerformingContext context) {
            long endTime = clock.nanoTime();
            releasePermits(context, result, endTime);
        }
    }
}
