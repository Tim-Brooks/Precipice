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
import net.uncontended.precipice.metrics.MetricCounter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;

public class MetricRegistryTest {

    @Mock
    private Service service;
    @Mock
    private ActionMetrics actionMetrics;
    @Mock
    private LatencyMetrics latencyMetrics;

    private MetricRegistry registry;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        when(service.getName()).thenReturn("Service Name");
        when(service.getActionMetrics()).thenReturn(actionMetrics);
        when(service.getLatencyMetrics()).thenReturn(latencyMetrics);
    }

    @Test
    public void testThing() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);

        registry = new MetricRegistry();
        when(actionMetrics.totalCountMetricCounter()).thenReturn(new MetricCounter());
        when(actionMetrics.metricCounterIterable(anyLong(), any(TimeUnit.class))).thenReturn(new
                ArrayList<MetricCounter>());


        registry.register(service);
        registry.setUpdateCallback(new PrecipiceFunction<Map<String, MetricRegistry.Summary>>() {
            @Override
            public void apply(Map<String, MetricRegistry.Summary> argument) {
                latch.countDown();
            }
        });

        latch.await();
        registry.shutdown();
    }
}
