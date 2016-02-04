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

import net.uncontended.precipice.*;
import net.uncontended.precipice.concurrent.*;
import net.uncontended.precipice.metrics.CountMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.time.Clock;

import java.util.ArrayList;
import java.util.List;

public class GuardRail<T extends Enum<T> & Result> {

    private final CountMetrics<T> countMetrics;
    private final LatencyMetrics<T> latencyMetrics;
    private final String name;
    private final Clock clock;
    private final FinishingCallback<T> finishingCallback;
    private volatile boolean isShutdown = false;
    private List<BackPressure> backPressureList;

    public GuardRail(String name, ControllerProperties<T> properties) {
        this(name, properties.actionMetrics(), properties.latencyMetrics(), properties.clock());
    }

    public GuardRail(String name, CountMetrics<T> countMetrics, LatencyMetrics<T> latencyMetrics, Clock clock) {
        this.countMetrics = countMetrics;
        this.latencyMetrics = latencyMetrics;
        this.name = name;
        this.clock = clock;
        backPressureList = new ArrayList<>();
        finishingCallback = new FinishingCallback<>(countMetrics, backPressureList, latencyMetrics, clock);
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

    public <R> PrecipicePromise<T, R> acquirePermitAndGetPromise() {
        return acquirePermitAndGetPromise(null);
    }

    public <R> PrecipicePromise<T, R> acquirePermitAndGetPromise(PrecipicePromise<T, R> externalPromise) {
        long startTime = clock.nanoTime();
        Rejected rejected = acquirePermitOrGetRejectedReason(startTime);
        if (rejected != null) {
            countMetrics.incrementRejectionCount(rejected, startTime);
            throw new RejectedException(rejected);
        }

        return getPromise(startTime, externalPromise);
    }

    public <R> PrecipicePromise<T, R> getPromise(long nanoTime) {
        return getPromise(nanoTime, null);
    }

    public <R> PrecipicePromise<T, R> getPromise(long nanoTime, Completable<T, R> externalCompletable) {
        Eventual<T, R> promise = new Eventual<>(nanoTime, externalCompletable);
        promise.internalOnComplete(finishingCallback);
        return promise;
    }

    public <R> Completable<T, R> acquirePermitAndGetCompletableContext(long nanoTime) {
        Rejected rejected = acquirePermitOrGetRejectedReason(nanoTime);
        if (rejected != null) {
            countMetrics.incrementRejectionCount(rejected, nanoTime);
            throw new RejectedException(rejected);
        }

        return getCompletableContext(nanoTime);
    }

    public <R> Completable<T, R> getCompletableContext(long nanoTime) {
        return getCompletableContext(nanoTime, null);
    }

    public <R> Completable<T, R> getCompletableContext(long nanoTime, Completable<T, R> completable) {
        CompletionContext<T, R> context = new CompletionContext<>(nanoTime, completable);
        context.internalOnComplete(finishingCallback);
        return context;
    }

    public void shutdown() {
        isShutdown = true;
    }

    public String getName() {
        return name;
    }

    public CountMetrics<T> getCountMetrics() {
        return countMetrics;
    }

    public LatencyMetrics<T> getLatencyMetrics() {
        return latencyMetrics;
    }

    public Clock getClock() {
        return clock;
    }

    private static class FinishingCallback<T extends Enum<T> & Result> implements PrecipiceFunction<T, PerformingContext> {

        private final CountMetrics<T> countMetrics;
        private final List<BackPressure> backPressureList;
        private final LatencyMetrics<T> latencyMetrics;
        private final Clock clock;

        private FinishingCallback(CountMetrics<T> countMetrics, List<BackPressure> backPressureList,
                                  LatencyMetrics<T> latencyMetrics, Clock clock) {
            this.countMetrics = countMetrics;
            this.backPressureList = backPressureList;
            this.latencyMetrics = latencyMetrics;
            this.clock = clock;
        }

        @Override
        public void apply(T status, PerformingContext context) {
            long endTime = clock.nanoTime();
            countMetrics.incrementMetricCount(status, endTime);
            latencyMetrics.recordLatency(status, endTime - context.startNanos(), endTime);
            for (BackPressure backPressure : backPressureList) {
                backPressure.releasePermit(1, status, endTime);
            }
        }
    }
}
