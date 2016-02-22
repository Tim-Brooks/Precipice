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

package net.uncontended.precipice.util;

import net.uncontended.precipice.BackPressure;
import net.uncontended.precipice.Failable;
import net.uncontended.precipice.GuardRail;
import net.uncontended.precipice.metrics.CountMetrics;
import net.uncontended.precipice.semaphore.UnlimitedSemaphore;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class Simulation {

    public static <R extends Enum<R> & Failable, V extends Enum<V>> void run(GuardRail<R, V> guardRail,
                                                                             Map<R, Runnable> resultToRunnable,
                                                                             Map<V, Runnable> rejectedToRunnable) {
        UnlimitedSemaphore<V> gauge = new UnlimitedSemaphore<>();
        wireUpGauge(guardRail, gauge);

        Random random = ThreadLocalRandom.current();
        int resultTypeCount = resultToRunnable.size();
        int rejectedTypeCount = rejectedToRunnable.size();
        int totalCount = resultTypeCount + rejectedTypeCount;
        List<R> resultTypes = new ArrayList<>(resultToRunnable.keySet());
        List<V> rejectedTypes = new ArrayList<>(rejectedToRunnable.keySet());
        long[] resultCounts = new long[resultTypeCount];
        long[] rejectedCounts = new long[rejectedTypeCount];

        int executions = random.nextInt(500) + 500;
        for (int i = 0; i < executions; ++i) {
            int j = random.nextInt(totalCount);

            long concurrencyLevel = gauge.currentConcurrencyLevel();
            assert concurrencyLevel == 0 : String.format("Expected concurrency of 0; Actual: %s", concurrencyLevel);

            if (j < resultTypeCount) {
                resultToRunnable.get(resultTypes.get(j)).run();
                ++resultCounts[j];
            } else {
                int rejectedIndex = j - resultTypeCount;
                rejectedToRunnable.get(rejectedTypes.get(rejectedIndex)).run();
                ++rejectedCounts[rejectedIndex];
            }

            concurrencyLevel = gauge.currentConcurrencyLevel();
            assert concurrencyLevel == 0 : String.format("Expected concurrency of 0; Actual: %s", concurrencyLevel);
        }

        assertMetrics("result", guardRail.getResultMetrics(), resultTypes, resultCounts);
        assertMetrics("rejected", guardRail.getRejectedMetrics(), rejectedTypes, rejectedCounts);
    }

    private static <R extends Enum<R> & Failable, V extends Enum<V>> void wireUpGauge(GuardRail<R, V> guardRail,
                                                                                      UnlimitedSemaphore<V> gauge) {
        try {
            Field f = guardRail.getClass().getDeclaredField("backPressureList");
            f.setAccessible(true);
            List<BackPressure<V>> backPressureList = (List<BackPressure<V>>) f.get(guardRail);
            backPressureList.add(gauge);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T extends Enum<T>> void assertMetrics(String testType, CountMetrics<T> metrics, List<T> types,
                                                          long[] counts) {
        for (int i = 0; i < types.size(); ++i) {
            for (int j = 0; j < 5; ++j) {

                T type = types.get(i);
                long actualCount = metrics.getMetricCount(type);
                long expectedCount = counts[i];

                if (expectedCount != actualCount && j != 4) {
                    System.out.println("here");
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(1));
                } else {
                    String message = String.format("Expected: %s %s counts to be returned for %s. Actual: %s.",
                            expectedCount, testType, type, actualCount);

                    assert actualCount == expectedCount : message;
                }
            }
        }
    }
}
