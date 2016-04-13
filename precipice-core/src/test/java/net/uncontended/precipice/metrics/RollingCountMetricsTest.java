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

import net.uncontended.precipice.result.TimeoutableResult;
import net.uncontended.precipice.time.Clock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class RollingCountMetricsTest {

    @Mock
    private Clock systemTime;

    private RollingCountMetrics<TimeoutableResult> metrics;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMetricsEdgeScenario() {
        long nanoTime = 0L * 1000L * 1000L;
        when(systemTime.nanoTime()).thenReturn(nanoTime);
        metrics = new RollingCountMetrics<>(TimeoutableResult.class, 1, 1, TimeUnit.SECONDS, systemTime);

        nanoTime = 1L * 1000L * 1000L;
        metrics.add(TimeoutableResult.SUCCESS, 1L, nanoTime);
        nanoTime = 2L * 1000L * 1000L;
        metrics.add(TimeoutableResult.SUCCESS, 1L, nanoTime);

        nanoTime = 999L * 1000L * 1000L;
        assertEquals(2, Accumulator.countForPeriod(metrics.intervals(nanoTime), TimeoutableResult.SUCCESS, 1, TimeUnit.SECONDS));

        nanoTime = 1000L * 1000L * 1000L;
        assertEquals(0, Accumulator.countForPeriod(metrics.intervals(nanoTime), TimeoutableResult.SUCCESS, 1, TimeUnit.SECONDS));
    }

    @Test
    public void testMetricsTrackingTwoSeconds() {
        TimeUnit unit = TimeUnit.SECONDS;
        long startTime = 0L;
        int slotsToTrack = 2;

        long nanoTime = startTime * 1000L * 1000L;

        when(systemTime.nanoTime()).thenReturn(nanoTime);
        metrics = new RollingCountMetrics<>(TimeoutableResult.class, slotsToTrack, 1, unit, systemTime);

        nanoTime = 1L * 1000L * 1000L;
        metrics.add(TimeoutableResult.ERROR, 1L, nanoTime);
        nanoTime = 2L * 1000L * 1000L;
        metrics.add(TimeoutableResult.ERROR, 1L, nanoTime);

        nanoTime = 999L * 1000L * 1000L;
        assertEquals(2, Accumulator.countForPeriod(metrics.intervals(nanoTime), TimeoutableResult.ERROR, 1, unit));

        nanoTime = 999L * 1000L * 1000L;
        assertEquals(2, Accumulator.countForPeriod(metrics.intervals(nanoTime), TimeoutableResult.ERROR, 2, unit));

        nanoTime = 1000L * 1000L * 1000L;
        assertEquals(0, Accumulator.countForPeriod(metrics.intervals(nanoTime), TimeoutableResult.ERROR, 1, unit));

        nanoTime = 1000L * 1000L * 1000L;
        assertEquals(2, Accumulator.countForPeriod(metrics.intervals(nanoTime), TimeoutableResult.ERROR, 2, unit));

        nanoTime = 2000L * 1000L * 1000L;
        assertEquals(0, Accumulator.countForPeriod(metrics.intervals(nanoTime), TimeoutableResult.ERROR, 1, unit));

        nanoTime = 2000L * 1000L * 1000L;
        assertEquals(0, Accumulator.countForPeriod(metrics.intervals(nanoTime), TimeoutableResult.ERROR, 2, unit));
    }

    @Test
    public void testMultipleWraps() {
        TimeUnit unit = TimeUnit.SECONDS;
        long startTime = 0L;
        long resolution = 1;
        long offsetTime = 50L;
        int slotsTracked = 10;
        long millisResolution = resolution * 1000;

        long nanoTime = startTime * 1000L * 1000L;
        when(systemTime.nanoTime()).thenReturn(nanoTime);
        metrics = new RollingCountMetrics<>(TimeoutableResult.class, slotsTracked, resolution, unit, systemTime);

        nanoTime = (offsetTime + millisResolution * 8) * 1000L * 1000L;
        metrics.add(TimeoutableResult.ERROR, 1L, nanoTime);

        nanoTime = (offsetTime + millisResolution * 20) * 1000L * 1000L;
        metrics.add(TimeoutableResult.SUCCESS, 1L, nanoTime);

        nanoTime = (offsetTime + millisResolution * 21) * 1000L * 1000L;
        IntervalIterator<PartitionedCount<TimeoutableResult>> intervals = metrics.intervals(nanoTime);
        Accumulator.Counts<TimeoutableResult> counts = Accumulator.countsForPeriod(intervals, resolution, unit);
        assertEquals(0, counts.get(TimeoutableResult.ERROR));
        assertEquals(0, counts.get(TimeoutableResult.SUCCESS));

        intervals = metrics.intervals(nanoTime);
        assertEquals(1, Accumulator.countForPeriod(intervals, TimeoutableResult.SUCCESS, resolution * 2, unit));
        assertEquals(1, metrics.totalCounter().getCount(TimeoutableResult.ERROR));
    }

    @Test
    public void testTotalCount() {
        TimeUnit unit = TimeUnit.SECONDS;
        long startTime = 0L;
        long resolution = 1;
        int slotsTracked = 10;
        when(systemTime.nanoTime()).thenReturn(startTime);
        metrics = new RollingCountMetrics<>(TimeoutableResult.class, slotsTracked, resolution, unit, systemTime);

        metrics.add(TimeoutableResult.SUCCESS, 1L, TimeUnit.SECONDS.toNanos(0));
        metrics.add(TimeoutableResult.SUCCESS, 1L, TimeUnit.SECONDS.toNanos(10));
        metrics.add(TimeoutableResult.SUCCESS, 1L, TimeUnit.SECONDS.toNanos(20));
        metrics.add(TimeoutableResult.SUCCESS, 1L, TimeUnit.SECONDS.toNanos(30));

        long currentTime = TimeUnit.SECONDS.toNanos(30);
        IntervalIterator<PartitionedCount<TimeoutableResult>> intervals = metrics.intervals(currentTime);
        assertEquals(1, Accumulator.countForPeriod(intervals, TimeoutableResult.SUCCESS, 10, TimeUnit.SECONDS));
        assertEquals(4, metrics.totalCounter().getCount(TimeoutableResult.SUCCESS));
    }

    @Test
    public void testMillisecondResolution() {
        TimeUnit unit = TimeUnit.MILLISECONDS;
        long startTime = 500L;
        long resolution = 100;
        long offsetTime = 550L;
        int slotsTracked = 1000;

        long nanoTime = startTime * 1000L * 1000L;
        when(systemTime.nanoTime()).thenReturn(nanoTime);
        metrics = new RollingCountMetrics<>(TimeoutableResult.class, slotsTracked, resolution, unit, systemTime);

        nanoTime = offsetTime * 1000L * 1000L;
        metrics.add(TimeoutableResult.SUCCESS, 1L, nanoTime);

        nanoTime = (offsetTime + resolution) * 1000L * 1000L;
        IntervalIterator<PartitionedCount<TimeoutableResult>> intervals = metrics.intervals(nanoTime);
        assertEquals(0, Accumulator.countForPeriod(intervals, TimeoutableResult.SUCCESS, resolution, unit));
        intervals = metrics.intervals(nanoTime);
        assertEquals(1, Accumulator.countForPeriod(intervals, TimeoutableResult.SUCCESS, resolution * 2, unit));

        nanoTime = (offsetTime + resolution * (slotsTracked - 1)) * 1000L * 1000L;
        intervals = metrics.intervals(nanoTime);
        assertEquals(1, Accumulator.countForPeriod(intervals, TimeoutableResult.SUCCESS, slotsTracked * resolution, unit));

        nanoTime = (offsetTime + resolution * slotsTracked) * 1000L * 1000L;
        intervals = metrics.intervals(nanoTime);
        assertEquals(0, Accumulator.countForPeriod(intervals, TimeoutableResult.SUCCESS, slotsTracked * resolution, unit));
    }

    @Test
    public void concurrentTest() throws Exception {
        when(systemTime.nanoTime()).thenReturn(1500L * 1000L * 1000L);
        metrics = new RollingCountMetrics<>(TimeoutableResult.class, 5, 1, TimeUnit.SECONDS, systemTime);

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

        long nanoTime = 6000L * 1000L * 1000L;
        IntervalIterator<PartitionedCount<TimeoutableResult>> intervals = metrics.intervals(nanoTime);
        Accumulator.Counts<TimeoutableResult> actual = Accumulator.countsForPeriod(intervals, 5, TimeUnit.SECONDS);
        assertEquals(5000, actual.get(TimeoutableResult.SUCCESS));
        assertEquals(5000, actual.get(TimeoutableResult.ERROR));
        assertEquals(5000, actual.get(TimeoutableResult.TIMEOUT));

        intervals = metrics.intervals(nanoTime);
        actual = Accumulator.countsForPeriod(intervals, 1, TimeUnit.SECONDS);
        assertEquals(1000, actual.get(TimeoutableResult.SUCCESS));
        assertEquals(1000, actual.get(TimeoutableResult.ERROR));
        assertEquals(1000, actual.get(TimeoutableResult.TIMEOUT));

        nanoTime = 6500L * 1000L * 1000L;
        intervals = metrics.intervals(nanoTime);
        actual = Accumulator.countsForPeriod(intervals, 5, TimeUnit.SECONDS);
        assertEquals(4000, actual.get(TimeoutableResult.SUCCESS));
        assertEquals(4000, actual.get(TimeoutableResult.ERROR));
        assertEquals(4000, actual.get(TimeoutableResult.TIMEOUT));
    }

    private static void fireThreads(final RollingCountMetrics<TimeoutableResult> metrics, final long nanoTime, int num) throws
            InterruptedException {
        final CountDownLatch latch = new CountDownLatch(num);

        for (int i = 0; i < num; ++i) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 100; ++j) {
                        metrics.add(TimeoutableResult.SUCCESS, 1L, nanoTime);
                        metrics.add(TimeoutableResult.ERROR, 1L, nanoTime);
                        metrics.add(TimeoutableResult.TIMEOUT, 1L, nanoTime);
                    }
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
    }

}
