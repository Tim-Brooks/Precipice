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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MetricRegistry {

    private int bucketCount = 6;
    private long period = 10;
    private TimeUnit unit = TimeUnit.SECONDS;
    private Map<String, Summary> services = new HashMap<>();
    private volatile PrecipiceFunction<Map<String, Summary>> callback;
    // TODO: Name thread
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public MetricRegistry() {
        executorService.scheduleAtFixedRate(new Task(), 0, period, unit);
    }

    public void register(Service service) {
        services.put(service.getName(), new Summary(service));
    }

    public Summary getSummary(String name) {
        Summary summary = services.get(name);
        if (summary == null) {
            throw new IllegalArgumentException("Service: " + name + " not registered.");
        }
        return summary;
    }

    public void setUpdateCallback(PrecipiceFunction<Map<String, Summary>> callback) {
        this.callback = callback;
    }

    public void shutdown() {
        executorService.shutdown();
    }


    private class Summary {
        private final Service service;

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

        private Summary(Service service) {
            this.service = service;
            this.remainingCapacity = service.remainingCapacity();
        }

        private void refresh() {
            pendingCount = service.pendingCount();
            remainingCapacity = service.remainingCapacity();
            successes = 0;
            errors = 0;
            timeouts = 0;
            maxConcurrency = 0;
            queueFull = 0;
            circuitOpen = 0;
            allRejected = 0;

            ActionMetrics actionMetrics = service.getActionMetrics();
            for (MetricCounter m : ((DefaultActionMetrics) actionMetrics).metricCounterIterator(period, unit)) {
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

            IntervalLatencyMetrics latencyMetrics = (IntervalLatencyMetrics) service.getLatencyMetrics();
            successLatency = latencyMetrics.intervalSnapshot(Metric.SUCCESS);
            errorLatency = latencyMetrics.intervalSnapshot(Metric.ERROR);
            timeoutLatency = latencyMetrics.intervalSnapshot(Metric.TIMEOUT);
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
