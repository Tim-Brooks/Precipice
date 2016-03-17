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

    /**
     * Acquire permits for task execution. If the acquisition is rejected then a reason
     * will be returned. If the acquisition is successful, null will be returned.
     *
     * @param number of permits to acquire
     * @return the rejected reason
     */
    public Rejected acquirePermits(long number) {
        return acquirePermits(number, clock.nanoTime());
    }

    /**
     * Acquire permits for task execution. If the acquisition is rejected then a reason
     * will be returned. If the acquisition is successful, null will be returned.
     *
     * @param number   of permits to acquire
     * @param nanoTime current nano time
     * @return the rejected reason
     */
    public Rejected acquirePermits(long number, long nanoTime) {
        for (int i = 0; i < backPressureList.size(); ++i) {
            BackPressure<Rejected> bp = backPressureList.get(i);
            Rejected rejected = bp.acquirePermit(number, nanoTime);
            if (rejected != null) {
                rejectedMetrics.add(rejected, number, nanoTime);
                for (int j = 0; j < i; ++j) {
                    backPressureList.get(j).releasePermit(number, nanoTime);
                }
                return rejected;
            }
        }
        return null;
    }

    /**
     * Release acquired permits without result. Since there is not a known result the result
     * count metrics and latency will not be updated.
     *
     * @param number of permits to release
     */
    public void releasePermitsWithoutResult(long number) {
        releasePermitsWithoutResult(number, clock.nanoTime());
    }

    /**
     * Release acquired permits without result. Since there is not a known result the result
     * count metrics and latency will not be updated.
     *
     * @param number   of permits to release
     * @param nanoTime current nano time
     */
    public void releasePermitsWithoutResult(long number, long nanoTime) {
        for (BackPressure backPressure : backPressureList) {
            backPressure.releasePermit(number, nanoTime);
        }
    }

    /**
     * Release acquired permits with known result. Since there is a known result the result
     * count metrics and latency will be updated.
     *
     * @param context context of the task execution
     * @param result  of the execution
     */
    public void releasePermits(ExecutionContext context, Result result) {
        releasePermits(context.permitCount(), result, context.startNanos(), clock.nanoTime());
    }

    /**
     * Release acquired permits with known result. Since there is a known result the result
     * count metrics and latency will be updated.
     *
     * @param context  context of the task execution
     * @param result   of the execution
     * @param nanoTime current nano time
     */
    public void releasePermits(ExecutionContext context, Result result, long nanoTime) {
        releasePermits(context.permitCount(), result, context.startNanos(), nanoTime);
    }

    /**
     * Release acquired permits with known result. Since there is a known result the result
     * count metrics and latency will be updated.
     *
     * @param number     of permits to release
     * @param result     of the execution
     * @param startNanos of the execution
     */
    public void releasePermits(long number, Result result, long startNanos) {
        releasePermits(number, result, startNanos, clock.nanoTime());
    }

    /**
     * Release acquired permits with known result. Since there is a known result the result
     * count metrics and latency will be updated.
     *
     * @param number     of permits to release
     * @param result     of the execution
     * @param startNanos of the execution
     * @param nanoTime   current nano time
     */
    public void releasePermits(long number, Result result, long startNanos, long nanoTime) {
        resultMetrics.add(result, number, nanoTime);
        latencyMetrics.recordLatency(result, number, nanoTime - startNanos, nanoTime);
        for (BackPressure backPressure : backPressureList) {
            backPressure.releasePermit(number, result, nanoTime);
        }
    }

    /**
     * Return a function that, when called with a result and execution context, will
     * release acquired permits.
     *
     * @return the function
     */
    public PrecipiceFunction<Result, ExecutionContext> releaseFunction() {
        return releaseFunction;
    }

    /**
     * Return the name of the GuardRail.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Return the result metrics used by the GuardRail.
     *
     * @return the result metrics
     */
    public CountMetrics<Result> getResultMetrics() {
        return resultMetrics;
    }

    /**
     * Return the rejected metrics used by the GuardRail.
     *
     * @return the rejected metrics
     */
    public CountMetrics<Rejected> getRejectedMetrics() {
        return rejectedMetrics;
    }

    /**
     * Return the latency metrics used by the GuardRail.
     *
     * @return the latency metrics
     */
    public LatencyMetrics<Result> getLatencyMetrics() {
        return latencyMetrics;
    }

    /**
     * Return the clock the GuardRail will refer to for time.
     *
     * @return the clock
     */
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
