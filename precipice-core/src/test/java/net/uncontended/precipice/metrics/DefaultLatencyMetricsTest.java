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

import org.HdrHistogram.AtomicHistogram;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class DefaultLatencyMetricsTest {

    @Mock
    private AtomicHistogram histogram;

    private DefaultLatencyMetrics metrics;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        metrics = new DefaultLatencyMetrics(histogram);
    }

    @Test
    public void latencyIsStoredInHistogram() {
        when(histogram.getMaxValue()).thenReturn(10L);
        when(histogram.getValueAtPercentile(50.0)).thenReturn(2L);
        when(histogram.getValueAtPercentile(90.0)).thenReturn(7L);
        when(histogram.getValueAtPercentile(99.0)).thenReturn(8L);
        when(histogram.getValueAtPercentile(99.9)).thenReturn(9L);
        when(histogram.getValueAtPercentile(99.99)).thenReturn(9L);
        when(histogram.getValueAtPercentile(99.999)).thenReturn(10L);

        LatencyBucket snapshot = metrics.getLatencySnapshot();

        assertEquals(10L, snapshot.latencyMax);
        assertEquals(2L, snapshot.latency50);
        assertEquals(7L, snapshot.latency90);
        assertEquals(8L, snapshot.latency99);
        assertEquals(9L, snapshot.latency999);
        assertEquals(9L, snapshot.latency9999);
        assertEquals(10L, snapshot.latency99999);

    }
}
