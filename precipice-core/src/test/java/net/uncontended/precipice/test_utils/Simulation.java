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

package net.uncontended.precipice.test_utils;

import net.uncontended.precipice.Failable;
import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.metrics.CountMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Simulation {

    public static <R extends Enum<R> & Failable, V extends Enum<V>> void run(GuardRail<R, V> guardRail,
                                                                             Map<R, Runnable> resultToRunnable,
                                                                             Map<V, Runnable> rejectedToRunnable) {
        Random random = ThreadLocalRandom.current();
        int resultTypeCount = resultToRunnable.size();
        int rejectedTypeCount = rejectedToRunnable.size();
        int totalCount = resultTypeCount + rejectedTypeCount;
        List<R> resultTypes = new ArrayList<>(resultToRunnable.keySet());
        List<V> rejectedTypes = new ArrayList<>(rejectedToRunnable.keySet());
        long[] resultCounts = new long[resultTypeCount];
        long[] rejectedCounts = new long[rejectedTypeCount];

        int executions = random.nextInt(1000) + 1000;
        for (int i = 0; i < executions; ++i) {
            int j = random.nextInt(totalCount);

            if (j < resultTypeCount) {
                resultToRunnable.get(resultTypes.get(j)).run();
                ++resultCounts[j];
            } else {
                rejectedToRunnable.get(rejectedTypes.get(j)).run();
                ++rejectedCounts[j];
            }
        }

        assertMetrics("result", guardRail.getResultMetrics(), resultTypes, resultCounts);
        assertMetrics("rejected", guardRail.getRejectedMetrics(), rejectedTypes, rejectedCounts);
    }

    private static <T extends Enum<T>> void assertMetrics(String testType, CountMetrics<T> metrics, List<T> types,
                                                          long[] counts) {
        for (int i = 0; i < types.size(); ++i) {
            T type = types.get(i);
            long actualCount = metrics.getMetricCount(type);
            long expectedCount = counts[i];

            String message = String.format("Expected: %s %s counts to be returned for %s. Actual: %s.",
                    expectedCount, testType, type, actualCount);
            assert actualCount == expectedCount : message;
        }
    }
}
