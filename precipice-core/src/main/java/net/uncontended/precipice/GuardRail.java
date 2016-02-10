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

import net.uncontended.precipice.backpressure.BPRejectedException;
import net.uncontended.precipice.backpressure.BPTotalCountMetrics;
import net.uncontended.precipice.concurrent.Completable;
import net.uncontended.precipice.concurrent.CompletionContext;
import net.uncontended.precipice.concurrent.Eventual;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.time.Clock;

import java.util.List;

public class GuardRail<Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>> {

    private final BPTotalCountMetrics<Result> resultMetrics;
    private final BPTotalCountMetrics<Rejected> rejectedMetrics;
    private final LatencyMetrics<Result> latencyMetrics;
    private final String name;
    private final Clock clock;
    private final FinishingCallback finishingCallback;
    private volatile boolean isShutdown = false;
    private List<BackPressure<Rejected>> backPressureList;

    public GuardRail(String name, BPTotalCountMetrics<Result> resultMetrics, BPTotalCountMetrics<Rejected> rejectedMetrics,
                     LatencyMetrics<Result> latencyMetrics, List<BackPressure<Rejected>> backPressureList, Clock clock) {
        this.resultMetrics = resultMetrics;
        this.rejectedMetrics = rejectedMetrics;
        this.latencyMetrics = latencyMetrics;
        this.name = name;
        this.clock = clock;
        this.backPressureList = backPressureList;
        finishingCallback = new FinishingCallback();
    }

    public Rejected acquirePermitOrGetRejectedReason(long nanoTime) {
        if (isShutdown) {
            throw new IllegalStateException("Service has been shutdown.");
        }

        for (int i = 0; i < backPressureList.size(); ++i) {
            Rejected rejected = backPressureList.get(i).acquirePermit(1, nanoTime);
            if (rejected != null) {
                for (int j = 0; j < i; ++j) {
                    backPressureList.get(j).releasePermit(1, nanoTime);
                }
            }
        }
        return null;
    }

    public <R> Eventual<Result, R> acquirePermitAndGetPromise() {
        return acquirePermitAndGetPromise(null);
    }

    public <R> Eventual<Result, R> acquirePermitAndGetPromise(PrecipicePromise<Result, R> externalPromise) {
        long startTime = clock.nanoTime();
        Rejected rejected = acquirePermitOrGetRejectedReason(startTime);
        if (rejected != null) {
            rejectedMetrics.incrementMetricCount(rejected, startTime);
            throw new BPRejectedException(rejected);
        }

        return getPromise(startTime, externalPromise);
    }

    public <R> Eventual<Result, R> getPromise(long nanoTime) {
        return getPromise(nanoTime, null);
    }

    public <R> Eventual<Result, R> getPromise(long nanoTime, Completable<Result, R> externalCompletable) {
        Eventual<Result, R> promise = new Eventual<>(nanoTime, externalCompletable);
        promise.internalOnComplete(finishingCallback);
        return promise;
    }

    public <R> Completable<Result, R> acquirePermitAndGetCompletableContext() {
        return acquirePermitAndGetCompletableContext(clock.nanoTime());
    }

    public <R> Completable<Result, R> acquirePermitAndGetCompletableContext(long nanoTime) {
        Rejected rejected = acquirePermitOrGetRejectedReason(nanoTime);
        if (rejected != null) {
            rejectedMetrics.incrementMetricCount(rejected, nanoTime);
            throw new BPRejectedException(rejected);
        }

        return getCompletableContext(nanoTime);
    }

    public void release(Result result, long starTime, long nanoTime) {
        resultMetrics.incrementMetricCount(result, nanoTime);
        latencyMetrics.recordLatency(result, nanoTime - starTime, nanoTime);
        for (BackPressure backPressure : backPressureList) {
            backPressure.releasePermit(1, result, nanoTime);
        }
    }

    public <R> CompletionContext<Result, R> getCompletableContext(long nanoTime) {
        return getCompletableContext(nanoTime, null);
    }

    public <R> CompletionContext<Result, R> getCompletableContext(long nanoTime, Completable<Result, R> completable) {
        CompletionContext<Result, R> context = new CompletionContext<>(nanoTime, completable);
        context.internalOnComplete(finishingCallback);
        return context;
    }

    public void shutdown() {
        isShutdown = true;
    }

    public String getName() {
        return name;
    }

    public BPTotalCountMetrics<Result> getResultMetrics() {
        return resultMetrics;
    }

    public BPTotalCountMetrics<Rejected> getRejectedMetrics() {
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
            release(result, context.startNanos(), endTime);
        }
    }
}
