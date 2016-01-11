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

import net.uncontended.precipice.SuperImpl;
import net.uncontended.precipice.SuperStatusInterface;
import net.uncontended.precipice.concurrent.LongAdder;

public class MetricCounter<T extends Enum<T> & SuperStatusInterface> {

    public static final MetricCounter<SuperImpl> NO_OP_COUNTER = new MetricCounter<SuperImpl>(SuperImpl.class) {
        @Override
        public void incrementMetric(SuperImpl metric) {
        }

        @Override
        public long getMetricCount(SuperImpl metric) {
            return 0L;
        }
    };

    private final LongAdder[] metrics;

    public MetricCounter(Class<T> clazz) {
        T[] metricValues = clazz.getEnumConstants();

        metrics = new LongAdder[metricValues.length];
        for (T metric : metricValues) {
            metrics[metric.ordinal()] = new LongAdder();
        }
    }

    public void incrementMetric(SuperImpl metric) {
        metrics[metric.ordinal()].increment();
    }

    public long getMetricCount(SuperImpl metric) {
        return metrics[metric.ordinal()].longValue();
    }

    public static <T extends Enum<T> & SuperStatusInterface> MetricCounter<T> noOpCounter(Class<T> clazz) {
        return new MetricCounter<T>(clazz) {
            @Override
            public void incrementMetric(SuperImpl metric) {
            }

            @Override
            public long getMetricCount(SuperImpl metric) {
                return 0L;
            }
        };
    }
}
