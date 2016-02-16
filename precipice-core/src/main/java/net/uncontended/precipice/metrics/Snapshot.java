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

import net.uncontended.precipice.TimeoutableResult;

import java.util.HashMap;
import java.util.Map;

public final class Snapshot {

    public static final String TOTAL_TOTAL = "total-total";
    public static final String TOTAL_SUCCESSES = "total-successes";
    public static final String TOTAL_TIMEOUTS = "total-timeouts";
    public static final String TOTAL_ERRORS = "total-errors";
    public static final String TOTAL = "total";
    public static final String SUCCESSES = "successes";
    public static final String TIMEOUTS = "timeouts";
    public static final String ERRORS = "errors";
    public static final String MAX_CONCURRENCY = "max-concurrency";
    public static final String CIRCUIT_OPEN = "circuit-open";
    public static final String ALL_REJECTED = "all-rejected";
    public static final String MAX_1_TOTAL = "max-1-total";
    public static final String MAX_1_SUCCESSES = "max-1-successes";
    public static final String MAX_1_TIMEOUTS = "max-1-timeouts";
    public static final String MAX_1_ERRORS = "max-1-errors";
    public static final String MAX_2_TOTAL = "max-2-total";
    public static final String MAX_2_SUCCESSES = "max-2-successes";
    public static final String MAX_2_TIMEOUTS = "max-2-timeouts";
    public static final String MAX_2_ERRORS = "max-2-errors";

    private Snapshot() {
    }

    public static Map<Object, Object> generate(MetricCounter<TimeoutableResult> totalCounter, Iterable<MetricCounter<TimeoutableResult>> slots) {

        long total = 0;
        long successes = 0;
        long timeouts = 0;
        long errors = 0;
        long maxConcurrency = 0;
        long circuitOpen = 0;
        long allRejected = 0;

        long maxTotal = 0;
        long maxSuccesses = 0;
        long maxTimeouts = 0;
        long maxErrors = 0;

        long max2Total = 0;
        long max2Successes = 0;
        long max2Timeouts = 0;
        long max2Errors = 0;

        long previousTotal = 0;
        long previousSuccesses = 0;
        long previousTimeouts = 0;
        long previousErrors = 0;
        for (MetricCounter<TimeoutableResult> metricCounter : slots) {
            long slotSuccesses = metricCounter.getMetricCount(TimeoutableResult.SUCCESS);
            long slotErrors = metricCounter.getMetricCount(TimeoutableResult.ERROR);
            long slotTimeouts = metricCounter.getMetricCount(TimeoutableResult.TIMEOUT);
            long slotTotal = slotSuccesses + slotErrors + slotTimeouts;

            total = total + slotTotal;
            maxTotal = Math.max(maxTotal, slotTotal);
            max2Total = Math.max(max2Total, slotTotal + previousTotal);

            successes = successes + slotSuccesses;
            maxSuccesses = Math.max(maxSuccesses, slotSuccesses);
            max2Successes = Math.max(max2Successes, slotSuccesses + previousSuccesses);

            timeouts = timeouts + slotTimeouts;
            maxTimeouts = Math.max(maxTimeouts, slotTimeouts);
            max2Timeouts = Math.max(max2Timeouts, slotTimeouts + previousTimeouts);

            errors = errors + slotErrors;
            maxErrors = Math.max(maxErrors, slotErrors);
            max2Errors = Math.max(max2Errors, slotErrors + previousErrors);

            previousTotal = slotTotal;
            previousSuccesses = slotSuccesses;
            previousTimeouts = slotTimeouts;
            previousErrors = slotErrors;
        }

        Map<Object, Object> metricsMap = new HashMap<>();
        putTotalCounts(totalCounter, metricsMap);

        metricsMap.put(TOTAL, total);
        metricsMap.put(SUCCESSES, successes);
        metricsMap.put(TIMEOUTS, timeouts);
        metricsMap.put(ERRORS, errors);
        metricsMap.put(MAX_CONCURRENCY, maxConcurrency);
        metricsMap.put(CIRCUIT_OPEN, circuitOpen);
        metricsMap.put(ALL_REJECTED, allRejected);

        metricsMap.put(MAX_1_TOTAL, maxTotal);
        metricsMap.put(MAX_1_SUCCESSES, maxSuccesses);
        metricsMap.put(MAX_1_TIMEOUTS, maxTimeouts);
        metricsMap.put(MAX_1_ERRORS, maxErrors);

        metricsMap.put(MAX_2_TOTAL, max2Total);
        metricsMap.put(MAX_2_SUCCESSES, max2Successes);
        metricsMap.put(MAX_2_TIMEOUTS, max2Timeouts);
        metricsMap.put(MAX_2_ERRORS, max2Errors);

        return metricsMap;
    }

    private static void putTotalCounts(MetricCounter<TimeoutableResult> totalCounter, Map<Object, Object> metricsMap) {
        long totalSuccesses = totalCounter.getMetricCount(TimeoutableResult.SUCCESS);
        long totalTimeouts = totalCounter.getMetricCount(TimeoutableResult.TIMEOUT);
        long totalErrors = totalCounter.getMetricCount(TimeoutableResult.ERROR);

        metricsMap.put(TOTAL_TOTAL, totalSuccesses + totalTimeouts + totalErrors);
        metricsMap.put(TOTAL_SUCCESSES, totalSuccesses);
        metricsMap.put(TOTAL_TIMEOUTS, totalTimeouts);
        metricsMap.put(TOTAL_ERRORS, totalErrors);
    }
}
