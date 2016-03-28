/*
 * Copyright 2015 Timothy Brooks
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
 */

package net.uncontended.precipice.reporting.registry;

import net.uncontended.precipice.Failable;
import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.metrics.CountMetrics;
import net.uncontended.precipice.metrics.IntervalIterable;
import net.uncontended.precipice.metrics.Rolling;

import java.util.Arrays;

public class Summary<Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>> {
    public final Class<Result> resultClazz;
    public final Class<Rejected> rejectedClazz;
    private final GuardRail<Result, Rejected> guardRail;

    private final long[] totalResultCounts;
    private final long[] resultCounts;

    private final long[] totalRejectedCounts;
    private final long[] rejectedCounts;

    private long currentStartEpoch;
    private long currentEndEpoch;

    private volatile int current = -1;
    private final Slice<Result, Rejected>[] slices;

    private final SummaryProperties properties;

    public Summary(SummaryProperties properties, GuardRail<Result, Rejected> guardRail) {
        this.guardRail = guardRail;

        resultClazz = guardRail.getResultMetrics().getMetricType();
        int resultLength = resultClazz.getEnumConstants().length;
        totalResultCounts = new long[resultLength];
        resultCounts = new long[resultLength];

        rejectedClazz = guardRail.getRejectedMetrics().getMetricType();
        int rejectedLength = rejectedClazz.getEnumConstants().length;
        totalRejectedCounts = new long[rejectedLength];
        rejectedCounts = new long[rejectedLength];

        slices = (Slice<Result, Rejected>[]) new Slice[properties.bufferSize];
        for (int i = 0; i < slices.length; ++i) {
            slices[i] = new Slice<>(resultClazz, rejectedClazz);
        }
        this.properties = properties;
    }

    public void refresh() {
        refresh(guardRail.getClock().nanoTime());
    }

    public void refresh(long nanoTime) {
        Arrays.fill(resultCounts, 0);
        Arrays.fill(rejectedCounts, 0);

        CountMetrics<Result> resultMetrics = guardRail.getResultMetrics();
        CountMetrics<Rejected> rejectedMetrics = guardRail.getRejectedMetrics();

        long localStartEpoch = Long.MAX_VALUE;
        long localEndEpoch = 0L;

        if (resultMetrics instanceof Rolling) {
            Rolling<CountMetrics<Result>> rollingMetrics = (Rolling<CountMetrics<Result>>) resultMetrics;
            IntervalIterable<CountMetrics<Result>> intervals = rollingMetrics.intervals(nanoTime);
            for (CountMetrics<Result> interval : intervals) {
                if (intervals.intervalStart() >= currentEndEpoch && intervals.intervalEnd() != -1) {
                    for (Result t : resultClazz.getEnumConstants()) {
                        resultCounts[t.ordinal()] += interval.getCount(t);
                    }
                    localStartEpoch = Math.min(localStartEpoch, intervals.intervalStart());
                    localEndEpoch = Math.max(localEndEpoch, intervals.intervalEnd());
                }
            }
        }

        if (rejectedMetrics instanceof Rolling) {
            Rolling<CountMetrics<Rejected>> rollingMetrics = (Rolling<CountMetrics<Rejected>>) rejectedMetrics;
            IntervalIterable<CountMetrics<Rejected>> intervals = rollingMetrics.intervals();
            for (CountMetrics<Rejected> interval : intervals) {
                if (intervals.intervalStart() >= currentEndEpoch && intervals.intervalEnd() != -1) {
                    for (Rejected t : rejectedClazz.getEnumConstants()) {
                        rejectedCounts[t.ordinal()] += interval.getCount(t);
                    }
                    localStartEpoch = Math.min(localStartEpoch, intervals.intervalStart());
                    localEndEpoch = Math.max(localEndEpoch, intervals.intervalEnd());
                }
            }
        }

        if (properties.accumulateTotalResults) {
            for (Result t : resultClazz.getEnumConstants()) {
                int metricIndex = t.ordinal();
                totalResultCounts[metricIndex] += resultCounts[metricIndex];
            }
        } else {
            for (Result t : resultClazz.getEnumConstants()) {
                int metricIndex = t.ordinal();
                totalResultCounts[metricIndex] = resultMetrics.getCount(t);
            }
        }

        if (properties.accumulateTotalRejections) {
            for (Rejected t : rejectedClazz.getEnumConstants()) {
                int metricIndex = t.ordinal();
                totalRejectedCounts[metricIndex] += rejectedCounts[metricIndex];
            }
        } else {
            for (Rejected t : rejectedClazz.getEnumConstants()) {
                int metricIndex = t.ordinal();
                totalRejectedCounts[metricIndex] = rejectedMetrics.getCount(t);
            }
        }
        currentStartEpoch = localStartEpoch;
        currentEndEpoch = localEndEpoch;
        updateSlice();
        ++current;

    }

    public Slice<Result, Rejected> currentSlice() {
        return slices[current % slices.length];
    }

    public Slice<Result, Rejected>[] getSlices() {
        return slices;
    }

    private void updateSlice() {
        Slice<Result, Rejected> slice = slices[current + 1];

        slice.startEpoch = currentStartEpoch;
        slice.endEpoch = currentEndEpoch;
        System.arraycopy(resultCounts, 0, slice.resultCounts, 0, resultCounts.length);
        System.arraycopy(totalResultCounts, 0, slice.totalResultCounts, 0, totalResultCounts.length);
        System.arraycopy(rejectedCounts, 0, slice.rejectedCounts, 0, rejectedCounts.length);
        System.arraycopy(totalRejectedCounts, 0, slice.totalRejectedCounts, 0, totalRejectedCounts.length);
    }
}
