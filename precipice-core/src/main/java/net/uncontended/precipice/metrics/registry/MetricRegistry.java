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

import net.uncontended.precipice.PrecipiceFunction;
import net.uncontended.precipice.Service;
import net.uncontended.precipice.metrics.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricRegistry {

    private int bucketCount = 6;
    private long period = 10;
    private TimeUnit unit = TimeUnit.SECONDS;
    private Map<String, Summary> services = new ConcurrentHashMap<>();
    private volatile PrecipiceFunction<Map<String, Summary>> callback;
    // TODO: Name thread
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public MetricRegistry() {
        executorService.scheduleAtFixedRate(new Task(), 0, period, unit);
    }

    public void register(Service service) {
        services.put(service.getName(), new Summary(service));
    }

    public boolean deregister(String name) {
        return null == services.remove(name);
    }

    public void setUpdateCallback(PrecipiceFunction<Map<String, Summary>> callback) {
        this.callback = callback;
    }

    public void shutdown() {
        executorService.shutdown();
    }


    private class Summary {
        private final Service service;

        public long totalPendingCount = 0;
        public long totalRemainingCapacity = 0;
        public long totalSuccesses = 0;
        public long totalErrors = 0;
        public long totalTimeouts = 0;
        public long totalMaxConcurrency = 0;
        public long totalQueueFull = 0;
        public long totalCircuitOpen = 0;
        public long totalAllRejected = 0;
        public long pendingCount = 0;
        public long remainingCapacity = 0;
        public long successes = 0;
        public long errors = 0;
        public long timeouts = 0;
        public long maxConcurrency = 0;
        public long queueFull = 0;
        public long circuitOpen = 0;
        public long allRejected = 0;
        public LatencySnapshot successLatency;
        public LatencySnapshot errorLatency;
        public LatencySnapshot timeoutLatency;

        public LatencySnapshot totalSuccessLatency;
        public LatencySnapshot totalErrorLatency;
        public LatencySnapshot totalTimeoutLatency;

        private Summary(Service service) {
            this.service = service;
            this.remainingCapacity = service.remainingCapacity();
        }

        private void refresh() {
            pendingCount = service.pendingCount();
            remainingCapacity = service.remainingCapacity();

            ActionMetrics actionMetrics = service.getActionMetrics();
            MetricCounter metricCounter = actionMetrics.totalCountMetricCounter();

            totalSuccesses = metricCounter.getMetric(Metric.SUCCESS).longValue();
            totalErrors = metricCounter.getMetric(Metric.ERROR).longValue();
            totalTimeouts = metricCounter.getMetric(Metric.TIMEOUT).longValue();
            totalMaxConcurrency = metricCounter.getMetric(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED).longValue();
            totalQueueFull = metricCounter.getMetric(Metric.QUEUE_FULL).longValue();
            totalCircuitOpen = metricCounter.getMetric(Metric.CIRCUIT_OPEN).longValue();
            totalAllRejected = metricCounter.getMetric(Metric.ALL_SERVICES_REJECTED).longValue();
            successes = 0;
            errors = 0;
            timeouts = 0;
            maxConcurrency = 0;
            queueFull = 0;
            circuitOpen = 0;
            allRejected = 0;

            for (MetricCounter m : actionMetrics.metricCounterIterable(period, unit)) {
                // TODO: Remove possibility of null
                if (m != null) {
                    successes += m.getMetric(Metric.SUCCESS).longValue();
                    errors += m.getMetric(Metric.ERROR).longValue();
                    timeouts += m.getMetric(Metric.TIMEOUT).longValue();
                    maxConcurrency += m.getMetric(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED).longValue();
                    queueFull += m.getMetric(Metric.QUEUE_FULL).longValue();
                    circuitOpen += m.getMetric(Metric.CIRCUIT_OPEN).longValue();
                    allRejected += m.getMetric(Metric.ALL_SERVICES_REJECTED).longValue();
                }
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

    private class Task implements Runnable {

        @Override
        public void run() {
            for (Summary summary : services.values()) {
                summary.refresh();
            }

            if (callback != null) {
                callback.apply(services);
            }
        }
    }
}
