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

package net.uncontended.precipice.metrics;

import net.uncontended.precipice.metrics.tools.IntervalIterator;
import net.uncontended.precipice.metrics.counts.PartitionedCount;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public final class Accumulator {

    public static <T extends Enum<T>> long count(Iterator<PartitionedCount<T>> intervals, T type) {
        long count = 0;
        PartitionedCount<T> metricCounter;
        while (intervals.hasNext()) {
            metricCounter = intervals.next();
            count += metricCounter.getCount(type);
        }
        return count;
    }

    public static <T extends Enum<T>> long countForPeriod(IntervalIterator<PartitionedCount<T>> intervals, T type,
                                                          long duration, TimeUnit unit) {
        intervals.limit(duration, unit);
        return count(intervals, type);
    }

    public static <T extends Enum<T>> Counts<T> counts(Iterator<PartitionedCount<T>> intervals) {
        Counts<T> counts = new Counts<>();
        PartitionedCount<T> metricCounter;
        Class<T> metricClazz = null;
        while (intervals.hasNext()) {
            metricCounter = intervals.next();
            long[] countArray = counts.array();
            if (countArray == null) {
                metricClazz = metricCounter.getMetricClazz();
                counts.init(metricClazz);
                countArray = counts.array();
            }

            for (T type : metricClazz.getEnumConstants()) {
                countArray[type.ordinal()] += metricCounter.getCount(type);
            }

        }
        return counts;
    }

    public static <T extends Enum<T>> Counts<T> countsForPeriod(IntervalIterator<PartitionedCount<T>> intervals,
                                                                long duration, TimeUnit unit) {
        intervals.limit(duration, unit);
        return counts(intervals);
    }

    public static class Counts<T extends Enum<T>> {
        private long[] counts = null;

        public long get(T type) {
            if (counts != null) {
                return counts[type.ordinal()];
            }
            return 0L;
        }

        void init(Class<T> clazz) {
            counts = new long[clazz.getEnumConstants().length];
        }

        long[] array() {
            return counts;
        }

    }
}
