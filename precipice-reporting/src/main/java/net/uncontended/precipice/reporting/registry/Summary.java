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

public class Summary<T extends Enum<T> & Failable> {
//    private final long period;
//    private final TimeUnit unit;
//    private final RollingCountMetrics<T> countMetrics;
//    private final Service service;
//
//    public long pendingCount = 0;
//    public long remainingCapacity = 0;
//
//    public final long[] totalMetricCounts;
//    public final long[] metricCounts;
//
//    public LatencySnapshot successLatency = LatencyMetrics.DEFAULT_SNAPSHOT;
//    public LatencySnapshot errorLatency = LatencyMetrics.DEFAULT_SNAPSHOT;
//    public LatencySnapshot timeoutLatency = LatencyMetrics.DEFAULT_SNAPSHOT;
//
//    public LatencySnapshot totalSuccessLatency = LatencyMetrics.DEFAULT_SNAPSHOT;
//    public LatencySnapshot totalErrorLatency = LatencyMetrics.DEFAULT_SNAPSHOT;
//    public LatencySnapshot totalTimeoutLatency = LatencyMetrics.DEFAULT_SNAPSHOT;
//    private final Class<T> metricType;
//
//    Summary(long period, TimeUnit unit, RollingCountMetrics<T> metrics, Service service) {
//        this.period = period;
//        this.unit = unit;
//        this.service = service;
//        countMetrics = metrics;
//        remainingCapacity = service.remainingCapacity();
//        metricType = metrics.getMetricType();
//        totalMetricCounts = new long[metricType.getEnumConstants().length];
//        metricCounts = new long[metricType.getEnumConstants().length];
//    }
//
//    public void refresh() {
//        pendingCount = service.pendingCount();
//        remainingCapacity = service.remainingCapacity();
//
//        MetricCounter<T> totalMetricCounter = countMetrics.totalCountMetricCounter();
//
//        for (T t : metricType.getEnumConstants()) {
//            int metricIndex = t.ordinal();
//            metricCounts[metricIndex] = 0;
//            totalMetricCounts[metricIndex] = totalMetricCounter.getMetricCount(t);
//        }
//
//        for (MetricCounter<T> m : countMetrics.metricCounters(period, unit)) {
//            for (T t : metricType.getEnumConstants()) {
//                metricCounts[t.ordinal()] += m.getMetricCount(t);
//            }
//        }
//
//        LatencyMetrics latencyMetrics = service.getLatencyMetrics();
//
//        if (metricType.isInstance(TimeoutableResult.class)) {
//            if (latencyMetrics instanceof IntervalLatencyMetrics) {
//                IntervalLatencyMetrics<TimeoutableResult> intervalMetrics = (IntervalLatencyMetrics<TimeoutableResult>) latencyMetrics;
//                successLatency = intervalMetrics.intervalSnapshot(TimeoutableResult.SUCCESS);
//                errorLatency = intervalMetrics.intervalSnapshot(TimeoutableResult.ERROR);
//                timeoutLatency = intervalMetrics.intervalSnapshot(TimeoutableResult.TIMEOUT);
//            }
//
//            totalSuccessLatency = latencyMetrics.latencySnapshot(TimeoutableResult.SUCCESS);
//            totalErrorLatency = latencyMetrics.latencySnapshot(TimeoutableResult.ERROR);
//            totalTimeoutLatency = latencyMetrics.latencySnapshot(TimeoutableResult.TIMEOUT);
//        }
//    }
}
