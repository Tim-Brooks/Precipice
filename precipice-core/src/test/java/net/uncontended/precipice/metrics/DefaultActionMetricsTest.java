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

import net.uncontended.precipice.utils.SystemTime;
import org.HdrHistogram.AtomicHistogram;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class DefaultActionMetricsTest {

    @Mock
    private SystemTime systemTime;

    private DefaultActionMetrics metrics;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMetricsEdgeScenario() {
        when(systemTime.nanoTime()).thenReturn(0L * 1000L * 1000L);
        metrics = new DefaultActionMetrics(1, 1, TimeUnit.SECONDS, systemTime);

        when(systemTime.nanoTime()).thenReturn(1L * 1000L * 1000L);
        metrics.incrementMetricCount(Metric.SUCCESS);
        when(systemTime.nanoTime()).thenReturn(2L * 1000L * 1000L);
        metrics.incrementMetricCount(Metric.SUCCESS);

        when(systemTime.nanoTime()).thenReturn(999L * 1000L * 1000L);
        assertEquals(2, metrics.getMetricCountForTimePeriod(Metric.SUCCESS, 1, TimeUnit.SECONDS));

        when(systemTime.nanoTime()).thenReturn(1000L * 1000L * 1000L);
        assertEquals(0, metrics.getMetricCountForTimePeriod(Metric.SUCCESS, 1, TimeUnit.SECONDS));
    }

    @Test
    public void testMetricsTrackingTwoSeconds() {
        TimeUnit unit = TimeUnit.SECONDS;
        long startTime = 0L;
        int slotsToTrack = 2;

        when(systemTime.nanoTime()).thenReturn(startTime * 1000L * 1000L);
        metrics = new DefaultActionMetrics(slotsToTrack, 1, unit, systemTime);

        when(systemTime.nanoTime()).thenReturn(1L * 1000L * 1000L);
        metrics.incrementMetricCount(Metric.ERROR);
        when(systemTime.nanoTime()).thenReturn(2L * 1000L * 1000L);
        metrics.incrementMetricCount(Metric.ERROR);

        when(systemTime.nanoTime()).thenReturn(999L * 1000L * 1000L);
        assertEquals(2, metrics.getMetricCountForTimePeriod(Metric.ERROR, 1, unit));

        when(systemTime.nanoTime()).thenReturn(999L * 1000L * 1000L);
        assertEquals(2, metrics.getMetricCountForTimePeriod(Metric.ERROR, 2, unit));

        when(systemTime.nanoTime()).thenReturn(1000L * 1000L * 1000L);
        assertEquals(0, metrics.getMetricCountForTimePeriod(Metric.ERROR, 1, unit));

        when(systemTime.nanoTime()).thenReturn(1000L * 1000L * 1000L);
        assertEquals(2, metrics.getMetricCountForTimePeriod(Metric.ERROR, 2, unit));

        when(systemTime.nanoTime()).thenReturn(2000L * 1000L * 1000L);
        assertEquals(0, metrics.getMetricCountForTimePeriod(Metric.ERROR, 1, unit));

        when(systemTime.nanoTime()).thenReturn(2000L * 1000L * 1000L);
        assertEquals(0, metrics.getMetricCountForTimePeriod(Metric.ERROR, 2, unit));
    }

    @Test
    public void testMultipleWraps() {
        TimeUnit unit = TimeUnit.SECONDS;
        long startTime = 0L;
        long resolution = 1;
        long offsetTime = 50L;
        int slotsTracked = 10;
        long millisResolution = resolution * 1000;

        when(systemTime.nanoTime()).thenReturn(startTime * 1000L * 1000L);
        metrics = new DefaultActionMetrics(slotsTracked, resolution, unit, systemTime);

        when(systemTime.nanoTime()).thenReturn((offsetTime + millisResolution * 8) * 1000L * 1000L);
        metrics.incrementMetricCount(Metric.ERROR);

        when(systemTime.nanoTime()).thenReturn((offsetTime + millisResolution * 20) * 1000L * 1000L);
        metrics.incrementMetricCount(Metric.SUCCESS);

        when(systemTime.nanoTime()).thenReturn((offsetTime + millisResolution * 21) * 1000L * 1000L);
        assertEquals(0, metrics.getMetricCountForTimePeriod(Metric.ERROR, resolution, unit));
        assertEquals(0, metrics.getMetricCountForTimePeriod(Metric.SUCCESS, resolution, unit));
        assertEquals(1, metrics.getMetricCountForTimePeriod(Metric.SUCCESS, resolution * 2, unit));
        assertEquals(1, metrics.getMetricCount(Metric.ERROR));
    }

    @Test
    public void testTotalCount() {
        TimeUnit unit = TimeUnit.SECONDS;
        long startTime = 0L;
        long resolution = 1;
        int slotsTracked = 10;
        when(systemTime.nanoTime()).thenReturn(startTime);
        metrics = new DefaultActionMetrics(slotsTracked, resolution, unit, systemTime);

        metrics.incrementMetricCount(Metric.SUCCESS, TimeUnit.SECONDS.toNanos(0));
        metrics.incrementMetricCount(Metric.SUCCESS, TimeUnit.SECONDS.toNanos(10));
        metrics.incrementMetricCount(Metric.SUCCESS, TimeUnit.SECONDS.toNanos(20));
        metrics.incrementMetricCount(Metric.SUCCESS, TimeUnit.SECONDS.toNanos(30));

        assertEquals(1, metrics.getMetricCountForTotalPeriod(Metric.SUCCESS, TimeUnit.SECONDS.toNanos(30)));
        assertEquals(4, metrics.getMetricCount(Metric.SUCCESS));

    }

    @Test
    public void testMillisecondResolution() {
        TimeUnit unit = TimeUnit.MILLISECONDS;
        long startTime = 500L;
        long resolution = 100;
        long offsetTime = 550L;
        int slotsTracked = 1000;

        when(systemTime.nanoTime()).thenReturn(startTime * 1000L * 1000L);
        metrics = new DefaultActionMetrics(slotsTracked, resolution, unit, systemTime);

        when(systemTime.nanoTime()).thenReturn(offsetTime * 1000L * 1000L);
        metrics.incrementMetricCount(Metric.SUCCESS);

        when(systemTime.nanoTime()).thenReturn((offsetTime + resolution) * 1000L * 1000L);
        assertEquals(0, metrics.getMetricCountForTimePeriod(Metric.SUCCESS, resolution, unit));
        assertEquals(1, metrics.getMetricCountForTimePeriod(Metric.SUCCESS, resolution * 2, unit));

        when(systemTime.nanoTime()).thenReturn((offsetTime + resolution * (slotsTracked - 1)) * 1000L * 1000L);
        assertEquals(1, metrics.getMetricCountForTimePeriod(Metric.SUCCESS, slotsTracked * resolution, unit));

        when(systemTime.nanoTime()).thenReturn((offsetTime + resolution * slotsTracked) * 1000L * 1000L);
        assertEquals(0, metrics.getMetricCountForTimePeriod(Metric.SUCCESS, slotsTracked * resolution, unit));
    }

    @Test
    public void testDoesNotThrowExceptionFromTooHighLatency() {
        TimeUnit unit = TimeUnit.MILLISECONDS;
        long resolution = 100;
        int slotsTracked = 1000;

        long trackableValue = TimeUnit.HOURS.toNanos(1);
        metrics = new DefaultActionMetrics(slotsTracked, resolution, unit, new AtomicHistogram(trackableValue, 2));
        metrics.incrementMetricAndRecordLatency(Metric.SUCCESS, TimeUnit.HOURS.toNanos(3), 0L);

        trackableValue = TimeUnit.MINUTES.toNanos(1);
        metrics = new DefaultActionMetrics(slotsTracked, resolution, unit, new AtomicHistogram(trackableValue, 2));
        metrics.incrementMetricAndRecordLatency(Metric.SUCCESS, TimeUnit.HOURS.toNanos(3), 0L);

    }

    @Test
    public void testSnapshot() {
        TimeUnit unit = TimeUnit.SECONDS;
        long startTime = 0L;
        long resolution = 1;
        long millisResolution = 1000L;
        long offsetTime = 50L;
        int slotsTracked = 10;

        long currentTime = startTime * 1000L * 1000L;
        when(systemTime.nanoTime()).thenReturn(currentTime);
        metrics = new DefaultActionMetrics(slotsTracked, resolution, unit, systemTime);

        currentTime = offsetTime * 1000L * 1000L;
        metrics.incrementMetricAndRecordLatency(Metric.SUCCESS, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.ERROR, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.TIMEOUT, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.QUEUE_FULL, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.CIRCUIT_OPEN, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.ALL_SERVICES_REJECTED, 1000L, currentTime);

        currentTime = (offsetTime + millisResolution) * 1000L * 1000L;
        metrics.incrementMetricAndRecordLatency(Metric.SUCCESS, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.SUCCESS, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.ERROR, 1000L, currentTime);

        currentTime = (offsetTime + millisResolution * 2) * 1000L * 1000L;
        metrics.incrementMetricAndRecordLatency(Metric.SUCCESS, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.ERROR, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.TIMEOUT, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.CIRCUIT_OPEN, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.CIRCUIT_OPEN, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.CIRCUIT_OPEN, 1000L, currentTime);

        currentTime = (offsetTime + millisResolution * 3) * 1000L * 1000L;
        metrics.incrementMetricAndRecordLatency(Metric.SUCCESS, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.ERROR, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.ERROR, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.ERROR, 1000L, currentTime);

        currentTime = (offsetTime + millisResolution * 4) * 1000L * 1000L;
        metrics.incrementMetricAndRecordLatency(Metric.CIRCUIT_OPEN, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.CIRCUIT_OPEN, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.TIMEOUT, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.QUEUE_FULL, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED, 1000L, currentTime);

        currentTime = (offsetTime + millisResolution * 5) * 1000L * 1000L;
        metrics.incrementMetricAndRecordLatency(Metric.CIRCUIT_OPEN, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.CIRCUIT_OPEN, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.TIMEOUT, 1000L, currentTime);
        metrics.incrementMetricAndRecordLatency(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED, 1000L, currentTime);

        when(systemTime.nanoTime()).thenReturn((offsetTime + millisResolution * 5) * 1000L * 1000L);

        Map<Object, Object> snapshot = metrics.snapshot(5, TimeUnit.SECONDS);
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

        assertEquals(1003L, snapshot.get(Snapshot.LATENCY_MAX));
        assertEquals(1002D, snapshot.get(Snapshot.LATENCY_MEAN));
        assertEquals(1003L, snapshot.get(Snapshot.LATENCY_50));
        assertEquals(1003L, snapshot.get(Snapshot.LATENCY_90));
        assertEquals(1003L, snapshot.get(Snapshot.LATENCY_99));
        assertEquals(1003L, snapshot.get(Snapshot.LATENCY_99_9));
        assertEquals(1003L, snapshot.get(Snapshot.LATENCY_99_99));
        assertEquals(1003L, snapshot.get(Snapshot.LATENCY_99_999));

        Map<Object, Object> snapshot2 = metrics.snapshot(4, TimeUnit.SECONDS);
        assertEquals(19L, snapshot2.get(Snapshot.TOTAL));
    }

    @Test
    public void concurrentTest() throws Exception {
        when(systemTime.nanoTime()).thenReturn(1500L * 1000L * 1000L);
        metrics = new DefaultActionMetrics(5, 1, TimeUnit.SECONDS, systemTime);

        long currentTime = 1980L * 1000L * 1000L;
        fireThreads(metrics, currentTime, 10);

        currentTime = 2620L * 1000L * 1000L;
        fireThreads(metrics, currentTime, 10);

        currentTime = 3500L * 1000L * 1000L;
        fireThreads(metrics, currentTime, 10);

        currentTime = 4820L * 1000L * 1000L;
        fireThreads(metrics, currentTime, 10);

        currentTime = 5600L * 1000L * 1000L;
        fireThreads(metrics, currentTime, 10);

        when(systemTime.nanoTime()).thenReturn(6000L * 1000L * 1000L);
        assertEquals(5000, metrics.getMetricCountForTimePeriod(Metric.CIRCUIT_OPEN, 5, TimeUnit.SECONDS));
        assertEquals(5000, metrics.getMetricCountForTimePeriod(Metric.SUCCESS, 5, TimeUnit.SECONDS));
        assertEquals(5000, metrics.getMetricCountForTimePeriod(Metric.ERROR, 5, TimeUnit.SECONDS));
        assertEquals(5000, metrics.getMetricCountForTimePeriod(Metric.TIMEOUT, 5, TimeUnit.SECONDS));
        assertEquals(5000, metrics.getMetricCountForTimePeriod(Metric.QUEUE_FULL, 5, TimeUnit.SECONDS));
        assertEquals(5000, metrics.getMetricCountForTimePeriod(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED, 5, TimeUnit.SECONDS));

        assertEquals(1000, metrics.getMetricCountForTimePeriod(Metric.CIRCUIT_OPEN, 1, TimeUnit.SECONDS));
        assertEquals(1000, metrics.getMetricCountForTimePeriod(Metric.SUCCESS, 1, TimeUnit.SECONDS));
        assertEquals(1000, metrics.getMetricCountForTimePeriod(Metric.ERROR, 1, TimeUnit.SECONDS));
        assertEquals(1000, metrics.getMetricCountForTimePeriod(Metric.TIMEOUT, 1, TimeUnit.SECONDS));
        assertEquals(1000, metrics.getMetricCountForTimePeriod(Metric.QUEUE_FULL, 1, TimeUnit.SECONDS));
        assertEquals(1000, metrics.getMetricCountForTimePeriod(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED, 1, TimeUnit.SECONDS));

        when(systemTime.nanoTime()).thenReturn(6500L * 1000L * 1000L);
        assertEquals(4000, metrics.getMetricCountForTimePeriod(Metric.CIRCUIT_OPEN, 5, TimeUnit.SECONDS));
        assertEquals(4000, metrics.getMetricCountForTimePeriod(Metric.SUCCESS, 5, TimeUnit.SECONDS));
        assertEquals(4000, metrics.getMetricCountForTimePeriod(Metric.ERROR, 5, TimeUnit.SECONDS));
        assertEquals(4000, metrics.getMetricCountForTimePeriod(Metric.TIMEOUT, 5, TimeUnit.SECONDS));
        assertEquals(4000, metrics.getMetricCountForTimePeriod(Metric.QUEUE_FULL, 5, TimeUnit.SECONDS));
        assertEquals(4000, metrics.getMetricCountForTimePeriod(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED, 5, TimeUnit.SECONDS));
    }

    private void fireThreads(final ActionMetrics metrics, final long nanoTime, int num) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(num);

        for (int i = 0; i < num; ++i) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 100; ++j) {
                        metrics.incrementMetricAndRecordLatency(Metric.SUCCESS, 1000L, nanoTime);
                        metrics.incrementMetricAndRecordLatency(Metric.ERROR,  2000L, nanoTime);
                        metrics.incrementMetricAndRecordLatency(Metric.TIMEOUT,  3000L, nanoTime);
                        metrics.incrementMetricAndRecordLatency(Metric.MAX_CONCURRENCY_LEVEL_EXCEEDED,  4000L, nanoTime);
                        metrics.incrementMetricAndRecordLatency(Metric.QUEUE_FULL,  5000L, nanoTime);
                        metrics.incrementMetricAndRecordLatency(Metric.CIRCUIT_OPEN,  6000L, nanoTime);
                    }
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
    }

}
