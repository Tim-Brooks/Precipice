/*
 * Copyright 2015 Timothy Brooks
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
 */

package net.uncontended.precipice.metrics.registry;

import net.uncontended.precipice.Service;
import net.uncontended.precipice.metrics.*;

import java.util.concurrent.TimeUnit;

public class Summary {
    private final long period;
    private final TimeUnit unit;
    private final Service service;

    public long pendingCount = 0;
    public long remainingCapacity = 0;

    public long totalSuccesses = 0;
    public long totalErrors = 0;
    public long totalTimeouts = 0;
    public long totalMaxConcurrency = 0;
    public long totalQueueFull = 0;
    public long totalCircuitOpen = 0;
    public long totalAllRejected = 0;
    public long successes = 0;
    public long errors = 0;
    public long timeouts = 0;
    public long maxConcurrency = 0;
    public long queueFull = 0;
    public long circuitOpen = 0;
    public long allRejected = 0;
    public LatencySnapshot successLatency = LatencyMetrics.DEFAULT_SNAPSHOT;
    public LatencySnapshot errorLatency = LatencyMetrics.DEFAULT_SNAPSHOT;
    public LatencySnapshot timeoutLatency = LatencyMetrics.DEFAULT_SNAPSHOT;

    public LatencySnapshot totalSuccessLatency = LatencyMetrics.DEFAULT_SNAPSHOT;
    public LatencySnapshot totalErrorLatency = LatencyMetrics.DEFAULT_SNAPSHOT;
    public LatencySnapshot totalTimeoutLatency = LatencyMetrics.DEFAULT_SNAPSHOT;

    Summary(long period, TimeUnit unit, Service service) {
        this.period = period;
        this.unit = unit;
        this.service = service;
        this.remainingCapacity = service.remainingCapacity();
    }

    public void refresh() {
        pendingCount = service.pendingCount();
        remainingCapacity = service.remainingCapacity();

        ActionMetrics actionMetrics = service.getActionMetrics();
        MetricCounter metricCounter = actionMetrics.totalCountMetricCounter();

        totalSuccesses = metricCounter.getMetricCount(Metric.SUCCESS);
        totalErrors = metricCounter.getMetricCount(Metric.ERROR);
        totalTimeouts = metricCounter.getMetricCount(Metric.TIMEOUT);
        totalMaxConcurrency = metricCounter.getMetricCount(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED);
        totalQueueFull = metricCounter.getMetricCount(Metric.QUEUE_FULL);
        totalCircuitOpen = metricCounter.getMetricCount(Metric.CIRCUIT_OPEN);
        totalAllRejected = metricCounter.getMetricCount(Metric.ALL_SERVICES_REJECTED);
        successes = 0;
        errors = 0;
        timeouts = 0;
        maxConcurrency = 0;
        queueFull = 0;
        circuitOpen = 0;
        allRejected = 0;

        for (MetricCounter m : actionMetrics.metricCounterIterable(period, unit)) {
            // TODO: Remove possibility of null
            successes += m.getMetricCount(Metric.SUCCESS);
            errors += m.getMetricCount(Metric.ERROR);
            timeouts += m.getMetricCount(Metric.TIMEOUT);
            maxConcurrency += m.getMetricCount(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED);
            queueFull += m.getMetricCount(Metric.QUEUE_FULL);
            circuitOpen += m.getMetricCount(Metric.CIRCUIT_OPEN);
            allRejected += m.getMetricCount(Metric.ALL_SERVICES_REJECTED);
        }

        LatencyMetrics latencyMetrics = service.getLatencyMetrics();
        if (latencyMetrics instanceof IntervalLatencyMetrics) {
            IntervalLatencyMetrics intervalMetrics = (IntervalLatencyMetrics) latencyMetrics;
            successLatency = intervalMetrics.intervalSnapshot(Metric.SUCCESS);
            errorLatency = intervalMetrics.intervalSnapshot(Metric.ERROR);
            timeoutLatency = intervalMetrics.intervalSnapshot(Metric.TIMEOUT);
        }

        totalSuccessLatency = latencyMetrics.latencySnapshot(Metric.SUCCESS);
        totalErrorLatency = latencyMetrics.latencySnapshot(Metric.ERROR);
        totalTimeoutLatency = latencyMetrics.latencySnapshot(Metric.TIMEOUT);
    }
}
