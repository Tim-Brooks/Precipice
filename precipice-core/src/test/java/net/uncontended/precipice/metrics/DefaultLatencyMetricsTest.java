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

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.Assert.assertEquals;

public class DefaultLatencyMetricsTest {

    private DefaultLatencyMetrics metrics;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        metrics = new DefaultLatencyMetrics();
    }

    @Test
    public void latencyIsStoredInHistogram() {
        Metric[] metricArray = new Metric[3];
        metricArray[0] = Metric.SUCCESS;
        metricArray[1] = Metric.ERROR;
        metricArray[2] = Metric.TIMEOUT;

        ThreadLocalRandom current = ThreadLocalRandom.current();
        for (int i = 1; i <= 100000; ++i) {
            int n = current.nextInt(3);
            metrics.recordLatency(metricArray[n], i);
        }

        LatencySnapshot snapshot = metrics.getLatencySnapshot();

        assertEquals(100, snapshot.latencyMax / 1000);
        assertEquals(50, snapshot.latency50 / 1000);
        assertEquals(90, snapshot.latency90 / 1000);
        assertEquals(99, snapshot.latency99 / 1000);
        assertEquals(100, snapshot.latency999 / 1000);
        assertEquals(100, snapshot.latency9999 / 1000);
        assertEquals(100, snapshot.latency99999 / 1000);
        assertEquals(50, (long) snapshot.latencyMean / 1000);

    }
}
