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

import net.uncontended.precipice.Failable;
import net.uncontended.precipice.metrics.MetricCounter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class HealthGauge {

    private final List<InternalGauge<?>> gauges = new ArrayList<>();

    public BPHealthSnapshot getHealth(long timePeriod, TimeUnit timeUnit, long nanoTime) {
        long total = 0;
        long failures = 0;

        for (InternalGauge<?> gauge : gauges) {
            gauge.refreshHealth(timePeriod, timeUnit, nanoTime);
            total = total + gauge.total;
            failures = failures + gauge.failures;
        }
        return new BPHealthSnapshot(total, failures);
    }

    public <Result extends Enum<Result> & Failable> void add(BPCountMetrics<Result> metrics) {
        gauges.add(new InternalGauge<>(metrics));
    }

    private class InternalGauge<Result extends Enum<Result> & Failable> {

        private final BPCountMetrics<Result> metrics;
        private final Class<Result> type;
        private long total = 0;
        private long failures = 0;

        private InternalGauge(BPCountMetrics<Result> metrics) {
            this.metrics = metrics;
            type = metrics.getMetricType();
        }

        // TODO: Not threadsafe
        private void refreshHealth(long timePeriod, TimeUnit timeUnit, long nanoTime) {
            total = 0;
            failures = 0;
            Iterable<MetricCounter<Result>> counters = metrics.metricCounters(timePeriod, timeUnit, nanoTime);

            for (MetricCounter<Result> metricCounter : counters) {
                for (Result result : type.getEnumConstants()) {
                    long metricCount = metricCounter.getMetricCount(result);
                    total += metricCount;

                    if (result.isFailure()) {
                        failures += metricCount;
                    }
                }
            }
        }
    }
}
