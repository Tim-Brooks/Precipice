/*
 * Copyright 2014 Timothy Brooks
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

package net.uncontended.precipice.metrics;

import net.uncontended.precipice.Rejected;
import net.uncontended.precipice.Result;
import net.uncontended.precipice.concurrent.LongAdder;

public class MetricCounter<T extends Enum<T> & Result> {

    private final LongAdder[] metrics;
    private final LongAdder[] rejectionMetrics;

    public MetricCounter(Class<T> clazz) {
        T[] metricValues = clazz.getEnumConstants();

        metrics = new LongAdder[metricValues.length];
        // TODO: Only track metrics for relevant statuses
        for (T metric : metricValues) {
            metrics[metric.ordinal()] = new LongAdder();
        }

        rejectionMetrics = new LongAdder[Rejected.values().length];
        for (Rejected reason : Rejected.values()) {
            rejectionMetrics[reason.ordinal()] = new LongAdder();
        }
    }

    public void incrementMetric(T metric) {
        metrics[metric.ordinal()].increment();
    }

    public long getMetricCount(T metric) {
        return metrics[metric.ordinal()].longValue();
    }

    public void incrementRejection(Rejected reason) {
        rejectionMetrics[reason.ordinal()].increment();

    }

    public long getRejectionCount(Rejected reason) {
        return rejectionMetrics[reason.ordinal()].longValue();
    }

    public static <T extends Enum<T> & Result> MetricCounter<T> noOpCounter(Class<T> clazz) {
        return new MetricCounter<T>(clazz) {
            @Override
            public void incrementMetric(T metric) {
            }

            @Override
            public long getMetricCount(T metric) {
                return 0L;
            }

            @Override
            public void incrementRejection(Rejected rejected) {
            }
        };
    }
}
