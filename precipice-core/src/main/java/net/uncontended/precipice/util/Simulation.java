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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public class Simulation<R extends Enum<R> & Failable> {

    private final Gauge gauge = new Gauge();
    private final GuardRail<R, SimulationRejected> guardRail;

    public Simulation(GuardRail<R, SimulationRejected> guardRail) {
        wireUpGauge(guardRail);
        this.guardRail = guardRail;
    }

    public void run(Map<R, Callable<Long>> resultToCallable) {
        Random random = ThreadLocalRandom.current();
        int resultTypeCount = resultToCallable.size();
        List<R> resultTypes = new ArrayList<>(resultToCallable.keySet());
        long[] resultCounts = new long[resultTypeCount];
        long rejectedCounts = 0;

        int executions = random.nextInt(500) + 500;
        for (int i = 0; i < executions; ++i) {
            int j = random.nextInt(resultTypeCount);
            if (random.nextInt(4) == 3) {
                ++rejectedCounts;
                gauge.allowNext = false;
            } else {
                ++resultCounts[j];
            }

            long concurrencyLevel = gauge.currentConcurrencyLevel();
            assert concurrencyLevel == 0 : String.format("Expected concurrency of 0; Actual: %s", concurrencyLevel);

            try {
                long permitNumber = resultToCallable.get(resultTypes.get(j)).call();
                assert permitNumber == gauge.last : String.format("Expected permit number of %s; Actual: %s",
                        permitNumber, gauge.last);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            concurrencyLevel = gauge.currentConcurrencyLevel();
            assert concurrencyLevel == 0 : String.format("Expected concurrency of 0; Actual: %s", concurrencyLevel);

            gauge.allowNext = true;
        }

        assertMetrics(guardRail.getResultMetrics(), resultTypes, resultCounts);
        assertRejectedMetrics(guardRail.getRejectedMetrics(), rejectedCounts);
    }

    private void wireUpGauge(GuardRail<R, SimulationRejected> guardRail) {
        try {
            Field f = guardRail.getClass().getDeclaredField("backPressureList");
            f.setAccessible(true);
            List<BackPressure<SimulationRejected>> backPressureList = (List<BackPressure<SimulationRejected>>) f.get(guardRail);
            backPressureList.add(gauge);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void assertRejectedMetrics(CountMetrics<SimulationRejected> rejectedMetrics, long rejectedCounts) {
        long actualCount = rejectedMetrics.getMetricCount(SimulationRejected.SIMULATION_REJECTION);
        String message = String.format("Expected: %s rejected counts to be returned for %s. Actual: %s.",
                rejectedCounts, SimulationRejected.SIMULATION_REJECTION, actualCount);

        assert actualCount == rejectedCounts : message;
    }

    private <T extends Enum<T>> void assertMetrics(CountMetrics<T> metrics, List<T> types, long[] counts) {
        for (int i = 0; i < types.size(); ++i) {

            T type = types.get(i);
            long actualCount = metrics.getMetricCount(type);
            long expectedCount = counts[i];
            String message = String.format("Expected: %s result counts to be returned for %s. Actual: %s.",
                    expectedCount, type, actualCount);

            assert actualCount == expectedCount : message;
        }
    }

    private class Gauge implements BackPressure<SimulationRejected> {

        private final AtomicLong concurrencyLevel = new AtomicLong(0);
        private long last = 0;
        private boolean allowNext = true;

        @Override
        public SimulationRejected acquirePermit(long number, long nanoTime) {
            last = number;
            if (allowNext) {
                concurrencyLevel.getAndAdd(number);
                return null;
            }
            return SimulationRejected.SIMULATION_REJECTION;
        }

        @Override
        public void releasePermit(long number, long nanoTime) {
            concurrencyLevel.getAndAdd(-number);
        }

        @Override
        public void releasePermit(long number, Failable result, long nanoTime) {
            concurrencyLevel.getAndAdd(-number);
        }

        @Override
        public <Result extends Enum<Result> & Failable> void registerResultMetrics(CountMetrics<Result> metrics) {
        }

        public long currentConcurrencyLevel() {
            return concurrencyLevel.get();
        }
    }
}

