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

package net.uncontended.precipice.backpressure;

import net.uncontended.precipice.PerformingContext;
import net.uncontended.precipice.PrecipiceFunction;
import net.uncontended.precipice.Result;
import net.uncontended.precipice.concurrent.Completable;
import net.uncontended.precipice.concurrent.CompletionContext;
import net.uncontended.precipice.concurrent.Eventual;
import net.uncontended.precipice.concurrent.PrecipicePromise;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.time.Clock;

import java.util.List;

public class GuardRail<Res extends Enum<Res> & Result, Rejected extends Enum<Rejected>> {

    private final BPCountMetrics<Res> resultMetrics;
    private final BPCountMetrics<Rejected> rejectedMetrics;
    private final LatencyMetrics<Res> latencyMetrics;
    private final String name;
    private final Clock clock;
    private final FinishingCallback<Res, Rejected> finishingCallback;
    private volatile boolean isShutdown = false;
    private List<BackPressure<Rejected>> backPressureList;


    public GuardRail(String name, BPCountMetrics<Res> resultMetrics, BPCountMetrics<Rejected> rejectedMetrics,
                     LatencyMetrics<Res> latencyMetrics, List<BackPressure<Rejected>> backPressureList, Clock clock) {
        this.resultMetrics = resultMetrics;
        this.rejectedMetrics = rejectedMetrics;
        this.latencyMetrics = latencyMetrics;
        this.name = name;
        this.clock = clock;
        this.backPressureList = backPressureList;
        finishingCallback = new FinishingCallback<>(resultMetrics, backPressureList, latencyMetrics, clock);
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

    public <R> PrecipicePromise<Res, R> acquirePermitAndGetPromise() {
        return acquirePermitAndGetPromise(null);
    }

    public <R> PrecipicePromise<Res, R> acquirePermitAndGetPromise(PrecipicePromise<Res, R> externalPromise) {
        long startTime = clock.nanoTime();
        Rejected rejected = acquirePermitOrGetRejectedReason(startTime);
        if (rejected != null) {
            rejectedMetrics.incrementMetricCount(rejected, startTime);
            throw new BPRejectedException(rejected);
        }

        return getPromise(startTime, externalPromise);
    }

    public <R> PrecipicePromise<Res, R> getPromise(long nanoTime) {
        return getPromise(nanoTime, null);
    }

    public <R> PrecipicePromise<Res, R> getPromise(long nanoTime, Completable<Res, R> externalCompletable) {
        Eventual<Res, R> promise = new Eventual<>(nanoTime, externalCompletable);
        promise.internalOnComplete(finishingCallback);
        return promise;
    }

    public <R> Completable<Res, R> acquirePermitAndGetCompletableContext(long nanoTime) {
        Rejected rejected = acquirePermitOrGetRejectedReason(nanoTime);
        if (rejected != null) {
            rejectedMetrics.incrementMetricCount(rejected, nanoTime);
            throw new BPRejectedException(rejected);
        }

        return getCompletableContext(nanoTime);
    }

    public <R> Completable<Res, R> getCompletableContext(long nanoTime) {
        return getCompletableContext(nanoTime, null);
    }

    public <R> Completable<Res, R> getCompletableContext(long nanoTime, Completable<Res, R> completable) {
        CompletionContext<Res, R> context = new CompletionContext<>(nanoTime, completable);
        context.internalOnComplete(finishingCallback);
        return context;
    }

    public void shutdown() {
        isShutdown = true;
    }

    public String getName() {
        return name;
    }

    public BPCountMetrics<Res> getResultMetrics() {
        return resultMetrics;
    }

    public BPCountMetrics<Rejected> getRejectedMetrics() {
        return rejectedMetrics;
    }

    public LatencyMetrics<Res> getLatencyMetrics() {
        return latencyMetrics;
    }

    public Clock getClock() {
        return clock;
    }

    private static class FinishingCallback<Res extends Enum<Res> & Result, Rejected extends Enum<Rejected>>
            implements PrecipiceFunction<Res, PerformingContext> {

        private final BPCountMetrics<Res> resultMetrics;
        private final List<BackPressure<Rejected>> backPressureList;
        private final LatencyMetrics<Res> latencyMetrics;
        private final Clock clock;

        private FinishingCallback(BPCountMetrics<Res> resultMetrics, List<BackPressure<Rejected>> backPressureList,
                                  LatencyMetrics<Res> latencyMetrics, Clock clock) {
            this.resultMetrics = resultMetrics;
            this.backPressureList = backPressureList;
            this.latencyMetrics = latencyMetrics;
            this.clock = clock;
        }

        @Override
        public void apply(Res result, PerformingContext context) {
            long endTime = clock.nanoTime();
            resultMetrics.incrementMetricCount(result, endTime);
            latencyMetrics.recordLatency(result, endTime - context.startNanos(), endTime);
            for (BackPressure backPressure : backPressureList) {
                backPressure.releasePermit(1, result, endTime);
            }
        }
    }
}
