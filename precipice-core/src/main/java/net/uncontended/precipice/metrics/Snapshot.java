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
import net.uncontended.precipice.Status;

import java.util.HashMap;
import java.util.Map;

public final class Snapshot {

    public static final String TOTAL_TOTAL = "total-total";
    public static final String TOTAL_SUCCESSES = "total-successes";
    public static final String TOTAL_TIMEOUTS = "total-timeouts";
    public static final String TOTAL_ERRORS = "total-errors";
    public static final String TOTAL_MAX_CONCURRENCY = "total-max-concurrency";
    public static final String TOTAL_QUEUE_FULL = "total-queue-full";
    public static final String TOTAL_CIRCUIT_OPEN = "total-circuit-open";
    public static final String TOTAL_ALL_REJECTED = "total-all-rejected";
    public static final String TOTAL = "total";
    public static final String SUCCESSES = "successes";
    public static final String TIMEOUTS = "timeouts";
    public static final String ERRORS = "errors";
    public static final String MAX_CONCURRENCY = "max-concurrency";
    public static final String QUEUE_FULL = "queue-full";
    public static final String CIRCUIT_OPEN = "circuit-open";
    public static final String ALL_REJECTED = "all-rejected";
    public static final String MAX_1_TOTAL = "max-1-total";
    public static final String MAX_1_SUCCESSES = "max-1-successes";
    public static final String MAX_1_TIMEOUTS = "max-1-timeouts";
    public static final String MAX_1_ERRORS = "max-1-errors";
    public static final String MAX_1_MAX_CONCURRENCY = "max-1-max-concurrency";
    public static final String MAX_1_QUEUE_FULL = "max-1-queue-full";
    public static final String MAX_1_CIRCUIT_OPEN = "max-1-circuit-open";
    public static final String MAX_1_ALL_REJECTED = "max-1-all-rejected";
    public static final String MAX_2_TOTAL = "max-2-total";
    public static final String MAX_2_SUCCESSES = "max-2-successes";
    public static final String MAX_2_TIMEOUTS = "max-2-timeouts";
    public static final String MAX_2_ERRORS = "max-2-errors";
    public static final String MAX_2_MAX_CONCURRENCY = "max-2-max-concurrency";
    public static final String MAX_2_QUEUE_FULL = "max-2-queue-full";
    public static final String MAX_2_CIRCUIT_OPEN = "max-2-circuit-open";
    public static final String MAX_2_ALL_REJECTED = "max-2-all-rejected";

    private Snapshot() {
    }

    public static Map<Object, Object> generate(MetricCounter<Status> totalCounter, Iterable<MetricCounter<Status>> slots) {

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
        long maxMaxConcurrency = 0;
        long maxCircuitOpen = 0;
        long maxAllRejected = 0;

        long max2Total = 0;
        long max2Successes = 0;
        long max2Timeouts = 0;
        long max2Errors = 0;
        long max2MaxConcurrency = 0;
        long max2CircuitOpen = 0;
        long max2AllRejected = 0;

        long previousTotal = 0;
        long previousSuccesses = 0;
        long previousTimeouts = 0;
        long previousErrors = 0;
        long previousMaxConcurrency = 0;
        long previousCircuitOpen = 0;
        long previousAllRejected = 0;
        for (MetricCounter<Status> metricCounter : slots) {
            long slotSuccesses = metricCounter.getMetricCount(Status.SUCCESS);
            long slotErrors = metricCounter.getMetricCount(Status.ERROR);
            long slotTimeouts = metricCounter.getMetricCount(Status.TIMEOUT);
            long slotMaxConcurrency = metricCounter.getRejectionCount(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED);
            long slotCircuitOpen = metricCounter.getRejectionCount(Rejected.CIRCUIT_OPEN);
            long slotAllRejected = metricCounter.getRejectionCount(Rejected.ALL_SERVICES_REJECTED);
            long slotTotal = slotSuccesses + slotErrors + slotTimeouts + slotMaxConcurrency + slotCircuitOpen
                    + slotAllRejected;

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

            maxConcurrency = slotMaxConcurrency + maxConcurrency;
            maxMaxConcurrency = Math.max(maxMaxConcurrency, slotMaxConcurrency);
            max2MaxConcurrency = Math.max(max2MaxConcurrency, slotMaxConcurrency + previousMaxConcurrency);
            
            circuitOpen = slotCircuitOpen + circuitOpen;
            maxCircuitOpen = Math.max(maxCircuitOpen, slotCircuitOpen);
            max2CircuitOpen = Math.max(max2CircuitOpen, slotCircuitOpen + previousCircuitOpen);

            allRejected = slotAllRejected + allRejected;
            maxAllRejected = Math.max(maxAllRejected, slotAllRejected);
            max2AllRejected = Math.max(max2AllRejected, slotAllRejected + previousAllRejected);

            previousTotal = slotTotal;
            previousSuccesses = slotSuccesses;
            previousTimeouts = slotTimeouts;
            previousErrors = slotErrors;
            previousMaxConcurrency = slotMaxConcurrency;
            previousCircuitOpen = slotCircuitOpen;
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
        metricsMap.put(MAX_1_MAX_CONCURRENCY, maxMaxConcurrency);
        metricsMap.put(MAX_1_CIRCUIT_OPEN, maxCircuitOpen);
        metricsMap.put(MAX_1_ALL_REJECTED, maxAllRejected);

        metricsMap.put(MAX_2_TOTAL, max2Total);
        metricsMap.put(MAX_2_SUCCESSES, max2Successes);
        metricsMap.put(MAX_2_TIMEOUTS, max2Timeouts);
        metricsMap.put(MAX_2_ERRORS, max2Errors);
        metricsMap.put(MAX_2_MAX_CONCURRENCY, max2MaxConcurrency);
        metricsMap.put(MAX_2_CIRCUIT_OPEN, max2CircuitOpen);
        metricsMap.put(MAX_2_ALL_REJECTED, max2AllRejected);

        return metricsMap;
    }

    private static void putTotalCounts(MetricCounter<Status> totalCounter, Map<Object, Object> metricsMap) {
        long totalSuccesses = totalCounter.getMetricCount(Status.SUCCESS);
        long totalTimeouts = totalCounter.getMetricCount(Status.TIMEOUT);
        long totalErrors = totalCounter.getMetricCount(Status.ERROR);
        long totalMaxConcurrency = totalCounter.getRejectionCount(Rejected.MAX_CONCURRENCY_LEVEL_EXCEEDED);
        long totalCircuitOpen = totalCounter.getRejectionCount(Rejected.CIRCUIT_OPEN);
        long totalAllRejected = totalCounter.getRejectionCount(Rejected.ALL_SERVICES_REJECTED);
        metricsMap.put(TOTAL_TOTAL, totalSuccesses + totalTimeouts + totalErrors + totalMaxConcurrency +
                totalCircuitOpen + totalAllRejected);
        metricsMap.put(TOTAL_SUCCESSES, totalSuccesses);
        metricsMap.put(TOTAL_TIMEOUTS, totalTimeouts);
        metricsMap.put(TOTAL_ERRORS, totalErrors);
        metricsMap.put(TOTAL_MAX_CONCURRENCY, totalMaxConcurrency);
        metricsMap.put(TOTAL_CIRCUIT_OPEN, totalCircuitOpen);
        metricsMap.put(TOTAL_ALL_REJECTED, totalAllRejected);
    }
}
