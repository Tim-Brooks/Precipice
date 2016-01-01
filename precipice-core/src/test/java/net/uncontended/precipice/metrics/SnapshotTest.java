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

import net.uncontended.precipice.time.Clock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class SnapshotTest {

    @Mock
    private Clock systemTime;

    private DefaultActionMetrics metrics;
    private final long offsetTime = 50L;
    private final long millisResolution = 1000L;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        TimeUnit unit = TimeUnit.SECONDS;
        long startTime = 0L;
        long resolution = 1;
        int slotsTracked = 10;

        when(systemTime.nanoTime()).thenReturn(startTime);
        metrics = new DefaultActionMetrics(slotsTracked, resolution, unit, systemTime);
        setupMetrics();
    }

    @Test
    public void testSnapshot() {
        when(systemTime.nanoTime()).thenReturn((offsetTime + millisResolution * 5) * 1000L * 1000L);

        Map<Object, Object> snapshot = Snapshot.generate(metrics.totalCountMetricCounter(),
                metrics.metricCounterIterable(5, TimeUnit.SECONDS));
        assertEquals(29L, snapshot.get(Snapshot.TOTAL_TOTAL));
        assertEquals(5L, snapshot.get(Snapshot.TOTAL_SUCCESSES));
        assertEquals(4L, snapshot.get(Snapshot.TOTAL_TIMEOUTS));
        assertEquals(6L, snapshot.get(Snapshot.TOTAL_ERRORS));
        assertEquals(8L, snapshot.get(Snapshot.TOTAL_CIRCUIT_OPEN));
        assertEquals(2L, snapshot.get(Snapshot.TOTAL_QUEUE_FULL));
        assertEquals(3L, snapshot.get(Snapshot.TOTAL_MAX_CONCURRENCY));
        assertEquals(1L, snapshot.get(Snapshot.TOTAL_ALL_REJECTED));
        assertEquals(22L, snapshot.get(Snapshot.TOTAL));
        assertEquals(4L, snapshot.get(Snapshot.SUCCESSES));
        assertEquals(3L, snapshot.get(Snapshot.TIMEOUTS));
        assertEquals(5L, snapshot.get(Snapshot.ERRORS));
        assertEquals(7L, snapshot.get(Snapshot.CIRCUIT_OPEN));
        assertEquals(1L, snapshot.get(Snapshot.QUEUE_FULL));
        assertEquals(2L, snapshot.get(Snapshot.MAX_CONCURRENCY));
        assertEquals(0L, snapshot.get(Snapshot.ALL_REJECTED));
        assertEquals(6L, snapshot.get(Snapshot.MAX_1_TOTAL));
        assertEquals(2L, snapshot.get(Snapshot.MAX_1_SUCCESSES));
        assertEquals(1L, snapshot.get(Snapshot.MAX_1_TIMEOUTS));
        assertEquals(3L, snapshot.get(Snapshot.MAX_1_ERRORS));
        assertEquals(3L, snapshot.get(Snapshot.MAX_1_CIRCUIT_OPEN));
        assertEquals(1L, snapshot.get(Snapshot.MAX_1_QUEUE_FULL));
        assertEquals(1L, snapshot.get(Snapshot.MAX_1_MAX_CONCURRENCY));
        assertEquals(0L, snapshot.get(Snapshot.MAX_1_ALL_REJECTED));
        assertEquals(10L, snapshot.get(Snapshot.MAX_2_TOTAL));
        assertEquals(3L, snapshot.get(Snapshot.MAX_2_SUCCESSES));
        assertEquals(2L, snapshot.get(Snapshot.MAX_2_TIMEOUTS));
        assertEquals(4L, snapshot.get(Snapshot.MAX_2_ERRORS));
        assertEquals(4L, snapshot.get(Snapshot.MAX_2_CIRCUIT_OPEN));
        assertEquals(1L, snapshot.get(Snapshot.MAX_2_QUEUE_FULL));
        assertEquals(2L, snapshot.get(Snapshot.MAX_2_MAX_CONCURRENCY));
        assertEquals(0L, snapshot.get(Snapshot.MAX_2_ALL_REJECTED));

//        assertEquals(10L, snapshot.get(Snapshot.LATENCY_MAX));
//        assertEquals(5D, snapshot.get(Snapshot.LATENCY_MEAN));
//        assertEquals(2L, snapshot.get(Snapshot.LATENCY_50));
//        assertEquals(7L, snapshot.get(Snapshot.LATENCY_90));
//        assertEquals(8L, snapshot.get(Snapshot.LATENCY_99));
//        assertEquals(9L, snapshot.get(Snapshot.LATENCY_99_9));
//        assertEquals(9L, snapshot.get(Snapshot.LATENCY_99_99));
//        assertEquals(10L, snapshot.get(Snapshot.LATENCY_99_999));
    }

    private void setupMetrics() {
        long currentTime = 0;

        currentTime = offsetTime * 1000L * 1000L;
        metrics.incrementMetricCount(Metric.SUCCESS, currentTime);
        metrics.incrementMetricCount(Metric.ERROR, currentTime);
        metrics.incrementMetricCount(Metric.TIMEOUT, currentTime);
        metrics.incrementMetricCount(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED, currentTime);
        metrics.incrementMetricCount(Metric.QUEUE_FULL, currentTime);
        metrics.incrementMetricCount(Metric.CIRCUIT_OPEN, currentTime);
        metrics.incrementMetricCount(Metric.ALL_SERVICES_REJECTED, currentTime);

        currentTime = (offsetTime + millisResolution) * 1000L * 1000L;
        metrics.incrementMetricCount(Metric.SUCCESS, currentTime);
        metrics.incrementMetricCount(Metric.SUCCESS, currentTime);
        metrics.incrementMetricCount(Metric.ERROR, currentTime);

        currentTime = (offsetTime + millisResolution * 2) * 1000L * 1000L;
        metrics.incrementMetricCount(Metric.SUCCESS, currentTime);
        metrics.incrementMetricCount(Metric.ERROR, currentTime);
        metrics.incrementMetricCount(Metric.TIMEOUT, currentTime);
        metrics.incrementMetricCount(Metric.CIRCUIT_OPEN, currentTime);
        metrics.incrementMetricCount(Metric.CIRCUIT_OPEN, currentTime);
        metrics.incrementMetricCount(Metric.CIRCUIT_OPEN, currentTime);

        currentTime = (offsetTime + millisResolution * 3) * 1000L * 1000L;
        metrics.incrementMetricCount(Metric.SUCCESS, currentTime);
        metrics.incrementMetricCount(Metric.ERROR, currentTime);
        metrics.incrementMetricCount(Metric.ERROR, currentTime);
        metrics.incrementMetricCount(Metric.ERROR, currentTime);

        currentTime = (offsetTime + millisResolution * 4) * 1000L * 1000L;
        metrics.incrementMetricCount(Metric.CIRCUIT_OPEN, currentTime);
        metrics.incrementMetricCount(Metric.CIRCUIT_OPEN, currentTime);
        metrics.incrementMetricCount(Metric.TIMEOUT, currentTime);
        metrics.incrementMetricCount(Metric.QUEUE_FULL, currentTime);
        metrics.incrementMetricCount(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED, currentTime);

        currentTime = (offsetTime + millisResolution * 5) * 1000L * 1000L;
        metrics.incrementMetricCount(Metric.CIRCUIT_OPEN, currentTime);
        metrics.incrementMetricCount(Metric.CIRCUIT_OPEN, currentTime);
        metrics.incrementMetricCount(Metric.TIMEOUT, currentTime);
        metrics.incrementMetricCount(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED, currentTime);

    }
}
