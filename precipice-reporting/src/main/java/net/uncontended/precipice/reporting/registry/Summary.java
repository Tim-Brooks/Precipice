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
import net.uncontended.precipice.metrics.*;
import net.uncontended.precipice.metrics.Rolling;

import java.util.concurrent.TimeUnit;

public class Summary<Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>> {
    private final long period;
    private final TimeUnit unit;
    private final GuardRail<Result, Rejected> guardRail;
    private final CountMetrics<Result> resultMetrics;

    public final long[] totalMetricCounts;
    public final long[] metricCounts;

    public final LatencySnapshot[] latencies;
    public final LatencySnapshot[] totalLatencies;

    // TODO: Add rejections

    Summary(long period, TimeUnit unit, GuardRail<Result, Rejected> guardRail) {
        this.period = period;
        this.unit = unit;
        this.guardRail = guardRail;
        resultMetrics = guardRail.getResultMetrics();
        int length = resultMetrics.getMetricType().getEnumConstants().length;
        totalMetricCounts = new long[length];
        metricCounts = new long[length];
        latencies = new LatencySnapshot[length];
        totalLatencies = new LatencySnapshot[length];
        for (int i = 0; i < length; ++i) {
            latencies[i] = LatencySnapshot.DEFAULT_SNAPSHOT;
            totalLatencies[i] = LatencySnapshot.DEFAULT_SNAPSHOT;
        }
    }

    public void refresh() {

        for (Result t : resultMetrics.getMetricType().getEnumConstants()) {
            int metricIndex = t.ordinal();
            metricCounts[metricIndex] = 0;
            totalMetricCounts[metricIndex] = resultMetrics.getCount(t);
        }

        if (resultMetrics instanceof RollingCountMetrics) {
            Rolling<CountMetrics<Result>> rollingMetrics = (Rolling<CountMetrics<Result>>) resultMetrics;
            for (CountMetrics<Result> m : rollingMetrics.forPeriod(period, unit)) {
                for (Result t : resultMetrics.getMetricType().getEnumConstants()) {
                    metricCounts[t.ordinal()] += m.getCount(t);
                }
            }
        }

        LatencyMetrics<Result> latencyMetrics = guardRail.getLatencyMetrics();

        if (latencyMetrics instanceof IntervalLatencyMetrics) {
            IntervalLatencyMetrics<Result> intervalMetrics = (IntervalLatencyMetrics<Result>) latencyMetrics;
            for (Result t : resultMetrics.getMetricType().getEnumConstants()) {
//                latencies[t.ordinal()] = intervalMetrics.intervalSnapshot(t);
            }
        }

        for (Result t : resultMetrics.getMetricType().getEnumConstants()) {
//            totalLatencies[t.ordinal()] = latencyMetrics.latencySnapshot(t);
        }
    }
}
