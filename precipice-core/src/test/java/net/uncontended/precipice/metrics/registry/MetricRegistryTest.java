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
import net.uncontended.precipice.metrics.ActionMetrics;
import net.uncontended.precipice.metrics.LatencyMetrics;
import net.uncontended.precipice.metrics.LatencySnapshot;
import net.uncontended.precipice.metrics.MetricCounter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class MetricRegistryTest {

    @Mock
    private Service service;
    @Mock
    private ActionMetrics actionMetrics;
    @Mock
    private LatencyMetrics latencyMetrics;

    private String serviceName = "Service Name";
    private MetricRegistry registry;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(service.getName()).thenReturn(serviceName);
        when(service.getActionMetrics()).thenReturn(actionMetrics);
        when(service.getLatencyMetrics()).thenReturn(latencyMetrics);
    }

    @Test
    public void testSummary() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        registry = new MetricRegistry(50, TimeUnit.MILLISECONDS);
        when(actionMetrics.totalCountMetricCounter()).thenReturn(new MetricCounter());
        when(actionMetrics.metricCounterIterable(50, TimeUnit.MILLISECONDS)).thenReturn(new ArrayList<MetricCounter>());

        final AtomicReference<MetricRegistry.Summary> summaryReference = new AtomicReference<>();

        registry.register(service);
        registry.setUpdateCallback(new PrecipiceFunction<Map<String, MetricRegistry.Summary>>() {
            @Override
            public void apply(Map<String, MetricRegistry.Summary> argument) {
                summaryReference.compareAndSet(null, argument.get(serviceName));
                latch.countDown();
            }
        });

        latch.await();
        registry.shutdown();

        MetricRegistry.Summary summary = summaryReference.get();
        assertEquals(0, summary.pendingCount);
        assertEquals(0, summary.totalPendingCount);
        assertEquals(0, summary.totalRemainingCapacity);
        assertEquals(0, summary.totalSuccesses);
        assertEquals(0, summary.totalErrors);
        assertEquals(0, summary.totalTimeouts);
        assertEquals(0, summary.totalMaxConcurrency);
        assertEquals(0, summary.totalQueueFull);
        assertEquals(0, summary.totalCircuitOpen);
        assertEquals(0, summary.totalAllRejected);
        assertEquals(0, summary.pendingCount);
        assertEquals(0, summary.remainingCapacity);
        assertEquals(0, summary.successes);
        assertEquals(0, summary.errors);
        assertEquals(0, summary.timeouts);
        assertEquals(0, summary.maxConcurrency);
        assertEquals(0, summary.queueFull);
        assertEquals(0, summary.circuitOpen);
        assertEquals(0, summary.allRejected);
        assertEquals(null, summary.successLatency);
        assertEquals(null, summary.errorLatency);
        assertEquals(null, summary.timeoutLatency);
        assertEquals(null, summary.totalSuccessLatency);
        assertEquals(null, summary.totalErrorLatency);
        assertEquals(null, summary.totalTimeoutLatency);
    }
}
