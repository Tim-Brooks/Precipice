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

import net.uncontended.precipice.Result;
import net.uncontended.precipice.metrics.MetricCounter;

import java.util.concurrent.TimeUnit;

public class HealthGauge<Res extends Enum<Res> & Result> {

    private final BPCountMetrics<Res>[] metricsArray;
    private final Class<Res> type;

    @SafeVarargs
    public HealthGauge(BPCountMetrics<Res>... metrics) {
        if (metrics.length == 0) {
            throw new IllegalArgumentException("Health gauge must include as least one result metrics.");
        }
        this.metricsArray = metrics;
        type = metrics[0].getMetricType();
    }

    public BPHealthSnapshot getHealth(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        long total = 0;
        long failures = 0;
        
        // TODO: Explore combining iterations iterations.
        for (BPCountMetrics<Res> metrics : metricsArray) {
            Iterable<MetricCounter<Res>> counters = metrics.metricCounterIterable(timePeriod, timeUnit, nanoTime);

            for (MetricCounter<Res> metricCounter : counters) {
                for (Res result : type.getEnumConstants()) {
                    long metricCount = metricCounter.getMetricCount(result);
                    total += metricCount;

                    if (result.isFailure()) {
                        failures += metricCount;
                    }
                }
            }
        }
        return new BPHealthSnapshot(total, failures);
    }
}
