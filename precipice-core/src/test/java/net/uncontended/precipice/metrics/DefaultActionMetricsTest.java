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

import net.uncontended.precipice.Status;
import net.uncontended.precipice.time.Clock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class DefaultActionMetricsTest {

    @Mock
    private Clock systemTime;

    private DefaultActionMetrics<Status> metrics;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMetricsEdgeScenario() {
        when(systemTime.nanoTime()).thenReturn(0L * 1000L * 1000L);
        metrics = new DefaultActionMetrics<>(Status.class, 1, 1, TimeUnit.SECONDS, systemTime);

        when(systemTime.nanoTime()).thenReturn(1L * 1000L * 1000L);
        metrics.incrementMetricCount(Status.SUCCESS);
        when(systemTime.nanoTime()).thenReturn(2L * 1000L * 1000L);
        metrics.incrementMetricCount(Status.SUCCESS);

        when(systemTime.nanoTime()).thenReturn(999L * 1000L * 1000L);
        assertEquals(2, metrics.getMetricCountForTimePeriod(Status.SUCCESS, 1, TimeUnit.SECONDS));

        when(systemTime.nanoTime()).thenReturn(1000L * 1000L * 1000L);
        assertEquals(0, metrics.getMetricCountForTimePeriod(Status.SUCCESS, 1, TimeUnit.SECONDS));
    }

    @Test
    public void testMetricsTrackingTwoSeconds() {
        TimeUnit unit = TimeUnit.SECONDS;
        long startTime = 0L;
        int slotsToTrack = 2;

        when(systemTime.nanoTime()).thenReturn(startTime * 1000L * 1000L);
        metrics = new DefaultActionMetrics<>(Status.class, slotsToTrack, 1, unit, systemTime);

        when(systemTime.nanoTime()).thenReturn(1L * 1000L * 1000L);
        metrics.incrementMetricCount(Status.ERROR);
        when(systemTime.nanoTime()).thenReturn(2L * 1000L * 1000L);
        metrics.incrementMetricCount(Status.ERROR);

        when(systemTime.nanoTime()).thenReturn(999L * 1000L * 1000L);
        assertEquals(2, metrics.getMetricCountForTimePeriod(Status.ERROR, 1, unit));

        when(systemTime.nanoTime()).thenReturn(999L * 1000L * 1000L);
        assertEquals(2, metrics.getMetricCountForTimePeriod(Status.ERROR, 2, unit));

        when(systemTime.nanoTime()).thenReturn(1000L * 1000L * 1000L);
        assertEquals(0, metrics.getMetricCountForTimePeriod(Status.ERROR, 1, unit));

        when(systemTime.nanoTime()).thenReturn(1000L * 1000L * 1000L);
        assertEquals(2, metrics.getMetricCountForTimePeriod(Status.ERROR, 2, unit));

        when(systemTime.nanoTime()).thenReturn(2000L * 1000L * 1000L);
        assertEquals(0, metrics.getMetricCountForTimePeriod(Status.ERROR, 1, unit));

        when(systemTime.nanoTime()).thenReturn(2000L * 1000L * 1000L);
        assertEquals(0, metrics.getMetricCountForTimePeriod(Status.ERROR, 2, unit));
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
        metrics = new DefaultActionMetrics<>(Status.class, slotsTracked, resolution, unit, systemTime);

        when(systemTime.nanoTime()).thenReturn((offsetTime + millisResolution * 8) * 1000L * 1000L);
        metrics.incrementMetricCount(Status.ERROR);

        when(systemTime.nanoTime()).thenReturn((offsetTime + millisResolution * 20) * 1000L * 1000L);
        metrics.incrementMetricCount(Status.SUCCESS);

        when(systemTime.nanoTime()).thenReturn((offsetTime + millisResolution * 21) * 1000L * 1000L);
        assertEquals(0, metrics.getMetricCountForTimePeriod(Status.ERROR, resolution, unit));
        assertEquals(0, metrics.getMetricCountForTimePeriod(Status.SUCCESS, resolution, unit));
        assertEquals(1, metrics.getMetricCountForTimePeriod(Status.SUCCESS, resolution * 2, unit));
        assertEquals(1, metrics.getMetricCount(Status.ERROR));
    }

    @Test
    public void testTotalCount() {
        TimeUnit unit = TimeUnit.SECONDS;
        long startTime = 0L;
        long resolution = 1;
        int slotsTracked = 10;
        when(systemTime.nanoTime()).thenReturn(startTime);
        metrics = new DefaultActionMetrics<>(Status.class, slotsTracked, resolution, unit, systemTime);

        metrics.incrementMetricCount(Status.SUCCESS, TimeUnit.SECONDS.toNanos(0));
        metrics.incrementMetricCount(Status.SUCCESS, TimeUnit.SECONDS.toNanos(10));
        metrics.incrementMetricCount(Status.SUCCESS, TimeUnit.SECONDS.toNanos(20));
        metrics.incrementMetricCount(Status.SUCCESS, TimeUnit.SECONDS.toNanos(30));

        assertEquals(1, metrics.getMetricCountForTimePeriod(Status.SUCCESS, 30, TimeUnit.NANOSECONDS));
        assertEquals(4, metrics.getMetricCount(Status.SUCCESS));
    }

    @Test
    public void testMillisecondResolution() {
        TimeUnit unit = TimeUnit.MILLISECONDS;
        long startTime = 500L;
        long resolution = 100;
        long offsetTime = 550L;
        int slotsTracked = 1000;

        when(systemTime.nanoTime()).thenReturn(startTime * 1000L * 1000L);
        metrics = new DefaultActionMetrics<>(Status.class, slotsTracked, resolution, unit, systemTime);

        when(systemTime.nanoTime()).thenReturn(offsetTime * 1000L * 1000L);
        metrics.incrementMetricCount(Status.SUCCESS);

        when(systemTime.nanoTime()).thenReturn((offsetTime + resolution) * 1000L * 1000L);
        assertEquals(0, metrics.getMetricCountForTimePeriod(Status.SUCCESS, resolution, unit));
        assertEquals(1, metrics.getMetricCountForTimePeriod(Status.SUCCESS, resolution * 2, unit));

        when(systemTime.nanoTime()).thenReturn((offsetTime + resolution * (slotsTracked - 1)) * 1000L * 1000L);
        assertEquals(1, metrics.getMetricCountForTimePeriod(Status.SUCCESS, slotsTracked * resolution, unit));

        when(systemTime.nanoTime()).thenReturn((offsetTime + resolution * slotsTracked) * 1000L * 1000L);
        assertEquals(0, metrics.getMetricCountForTimePeriod(Status.SUCCESS, slotsTracked * resolution, unit));
    }

    @Test
    public void testDoesNotThrowExceptionFromTooHighLatency() {
        TimeUnit unit = TimeUnit.MILLISECONDS;
//        long resolution = 100;
//        int slotsTracked = 1000;
//
//        long trackableValue = TimeUnit.HOURS.toNanos(1);
//        metrics = new DefaultActionMetrics(slotsTracked, resolution, unit, new AtomicHistogram(trackableValue, 2));
//        metrics.incrementMetricAndRecordLatency(Status.SUCCESS, TimeUnit.HOURS.toNanos(3), 0L);
//
//        trackableValue = TimeUnit.MINUTES.toNanos(1);
//        metrics = new DefaultActionMetrics(slotsTracked, resolution, unit, new AtomicHistogram(trackableValue, 2));
//        metrics.incrementMetricAndRecordLatency(Status.SUCCESS, TimeUnit.HOURS.toNanos(3), 0L);
    }

    @Test
    public void concurrentTest() throws Exception {
        when(systemTime.nanoTime()).thenReturn(1500L * 1000L * 1000L);
        metrics = new DefaultActionMetrics<>(Status.class, 5, 1, TimeUnit.SECONDS, systemTime);

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
        assertEquals(5000, metrics.getMetricCountForTimePeriod(Status.CIRCUIT_OPEN, 5, TimeUnit.SECONDS));
        assertEquals(5000, metrics.getMetricCountForTimePeriod(Status.SUCCESS, 5, TimeUnit.SECONDS));
        assertEquals(5000, metrics.getMetricCountForTimePeriod(Status.ERROR, 5, TimeUnit.SECONDS));
        assertEquals(5000, metrics.getMetricCountForTimePeriod(Status.TIMEOUT, 5, TimeUnit.SECONDS));
        assertEquals(5000, metrics.getMetricCountForTimePeriod(Status.QUEUE_FULL, 5, TimeUnit.SECONDS));
        assertEquals(5000, metrics.getMetricCountForTimePeriod(Status.MAX_CONCURRENCY_LEVEL_EXCEEDED, 5, TimeUnit.SECONDS));

        assertEquals(1000, metrics.getMetricCountForTimePeriod(Status.CIRCUIT_OPEN, 1, TimeUnit.SECONDS));
        assertEquals(1000, metrics.getMetricCountForTimePeriod(Status.SUCCESS, 1, TimeUnit.SECONDS));
        assertEquals(1000, metrics.getMetricCountForTimePeriod(Status.ERROR, 1, TimeUnit.SECONDS));
        assertEquals(1000, metrics.getMetricCountForTimePeriod(Status.TIMEOUT, 1, TimeUnit.SECONDS));
        assertEquals(1000, metrics.getMetricCountForTimePeriod(Status.QUEUE_FULL, 1, TimeUnit.SECONDS));
        assertEquals(1000, metrics.getMetricCountForTimePeriod(Status.MAX_CONCURRENCY_LEVEL_EXCEEDED, 1, TimeUnit.SECONDS));

        when(systemTime.nanoTime()).thenReturn(6500L * 1000L * 1000L);
        assertEquals(4000, metrics.getMetricCountForTimePeriod(Status.CIRCUIT_OPEN, 5, TimeUnit.SECONDS));
        assertEquals(4000, metrics.getMetricCountForTimePeriod(Status.SUCCESS, 5, TimeUnit.SECONDS));
        assertEquals(4000, metrics.getMetricCountForTimePeriod(Status.ERROR, 5, TimeUnit.SECONDS));
        assertEquals(4000, metrics.getMetricCountForTimePeriod(Status.TIMEOUT, 5, TimeUnit.SECONDS));
        assertEquals(4000, metrics.getMetricCountForTimePeriod(Status.QUEUE_FULL, 5, TimeUnit.SECONDS));
        assertEquals(4000, metrics.getMetricCountForTimePeriod(Status.MAX_CONCURRENCY_LEVEL_EXCEEDED, 5, TimeUnit.SECONDS));
    }

    private static void fireThreads(final ActionMetrics<Status> metrics, final long nanoTime, int num) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(num);

        for (int i = 0; i < num; ++i) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 100; ++j) {
                        metrics.incrementMetricCount(Status.SUCCESS, nanoTime);
                        metrics.incrementMetricCount(Status.ERROR, nanoTime);
                        metrics.incrementMetricCount(Status.TIMEOUT, nanoTime);
                        metrics.incrementMetricCount(Status.MAX_CONCURRENCY_LEVEL_EXCEEDED, nanoTime);
                        metrics.incrementMetricCount(Status.QUEUE_FULL, nanoTime);
                        metrics.incrementMetricCount(Status.CIRCUIT_OPEN, nanoTime);
                    }
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
    }

}
