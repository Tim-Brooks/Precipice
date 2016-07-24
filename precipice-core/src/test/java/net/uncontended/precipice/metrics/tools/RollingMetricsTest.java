/*
* Copyright 2016 Timothy Brooks
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

package net.uncontended.precipice.metrics.tools;

import net.uncontended.precipice.metrics.IntervalIterator;
import net.uncontended.precipice.time.Clock;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;

public class RollingMetricsTest {

    private static final AtomicLong default0 = new AtomicLong(0);

    @Mock
    private Clock systemTime;
    private RollingMetrics<AtomicLong> metrics;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testMetricsEdgeScenario() {
        long startTime = ThreadLocalRandom.current().nextLong();
        CircularBuffer<AtomicLong> buffer = new CircularBuffer<>(1, TimeUnit.SECONDS.toNanos(1), startTime);
        metrics = new RollingMetrics<AtomicLong>(new LongAllocator(), buffer, systemTime);

        long nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(1);
        metrics.current(nanoTime).getAndIncrement();
        nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(2);
        metrics.current(nanoTime).getAndIncrement();

        nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(999);
        assertEquals(2, countForPeriod(metrics.intervals(nanoTime), 1, TimeUnit.SECONDS));

        nanoTime = startTime + TimeUnit.SECONDS.toNanos(1);
        assertEquals(0, countForPeriod(metrics.intervalsWithDefault(nanoTime, default0), 1, TimeUnit.SECONDS));
    }

    @Test
    public void testMetricsTrackingTwoSeconds() {
        long startTime = ThreadLocalRandom.current().nextLong();
        CircularBuffer<AtomicLong> buffer = new CircularBuffer<>(2, TimeUnit.SECONDS.toNanos(1), startTime);
        metrics = new RollingMetrics<AtomicLong>(new LongAllocator(), buffer, systemTime);

        long nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(1);
        metrics.current(nanoTime).getAndIncrement();
        nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(2);
        metrics.current(nanoTime).getAndIncrement();

        nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(999L);
        assertEquals(2, countForPeriod(metrics.intervalsWithDefault(nanoTime, default0), 1, TimeUnit.SECONDS));
        assertEquals(2, countForPeriod(metrics.intervalsWithDefault(nanoTime, default0), 2, TimeUnit.SECONDS));

        nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(1000L);
        assertEquals(0, countForPeriod(metrics.intervalsWithDefault(nanoTime, default0), 1, TimeUnit.SECONDS));
        assertEquals(2, countForPeriod(metrics.intervalsWithDefault(nanoTime, default0), 2, TimeUnit.SECONDS));

        nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(2000L);
        assertEquals(0, countForPeriod(metrics.intervalsWithDefault(nanoTime, default0), 1, TimeUnit.SECONDS));
        assertEquals(0, countForPeriod(metrics.intervalsWithDefault(nanoTime, default0), 2, TimeUnit.SECONDS));
    }

    @Test
    public void testMultipleWraps() {
        long startTime = ThreadLocalRandom.current().nextLong();
        long offsetTime = startTime + TimeUnit.MILLISECONDS.toNanos(ThreadLocalRandom.current().nextLong(900L));

        CircularBuffer<AtomicLong> buffer = new CircularBuffer<>(10, TimeUnit.SECONDS.toNanos(1), startTime);
        metrics = new RollingMetrics<AtomicLong>(new LongAllocator(), buffer, systemTime);

        long nanoTime;
        for (int i = 0; i < 10; ++i) {
            nanoTime = offsetTime + TimeUnit.SECONDS.toNanos(i);
            metrics.current(nanoTime).getAndIncrement();
        }

        nanoTime = offsetTime + TimeUnit.SECONDS.toNanos(20);
        metrics.current(nanoTime).getAndIncrement();

        nanoTime = offsetTime + TimeUnit.SECONDS.toNanos(21);
        IntervalIterator<AtomicLong> intervals = metrics.intervalsWithDefault(nanoTime, default0);
        countForPeriod(intervals, 1, TimeUnit.SECONDS);
        assertEquals(0, countForPeriod(intervals, 1, TimeUnit.SECONDS));

        intervals.reset(nanoTime);
        assertEquals(1, countForPeriod(intervals, 2, TimeUnit.SECONDS));
    }

    @Test
    public void testMillisecondResolution() {
        long startTime = ThreadLocalRandom.current().nextLong();
        long offsetTime = startTime + ThreadLocalRandom.current().nextLong(100);

        CircularBuffer<AtomicLong> buffer = new CircularBuffer<>(1000, TimeUnit.MILLISECONDS.toNanos(100), startTime);
        metrics = new RollingMetrics<AtomicLong>(new LongAllocator(), buffer, systemTime);

        long nanoTime = offsetTime;
        metrics.current(nanoTime).getAndIncrement();

        nanoTime = offsetTime + TimeUnit.MILLISECONDS.toNanos(100);
        IntervalIterator<AtomicLong> intervals = metrics.intervalsWithDefault(nanoTime, default0);
        assertEquals(0, countForPeriod(intervals, 100, TimeUnit.MILLISECONDS));
        intervals.reset(nanoTime);
        assertEquals(1, countForPeriod(intervals, 200, TimeUnit.MILLISECONDS));

        nanoTime = offsetTime + TimeUnit.MILLISECONDS.toNanos(100 * 999);
        intervals.reset(nanoTime);
        assertEquals(1, countForPeriod(intervals, 1000 * 100, TimeUnit.MILLISECONDS));

        nanoTime = offsetTime + TimeUnit.MILLISECONDS.toNanos(100 * 1000);
        intervals.reset(nanoTime);
        assertEquals(0, countForPeriod(intervals, 1000 * 100, TimeUnit.MILLISECONDS));
    }
//
//    @Test
//    public void concurrentTest() throws Exception {
//        when(systemTime.nanoTime()).thenReturn(1500L * 1000L * 1000L);
//        metrics = new RollingCountMetrics<>(TimeoutableResult.class, 5, 1, TimeUnit.SECONDS, systemTime);
//
//        long currentTime = 1980L * 1000L * 1000L;
//        fireThreads(metrics, currentTime, 10);
//
//        currentTime = 2620L * 1000L * 1000L;
//        fireThreads(metrics, currentTime, 10);
//
//        currentTime = 3500L * 1000L * 1000L;
//        fireThreads(metrics, currentTime, 10);
//
//        currentTime = 4820L * 1000L * 1000L;
//        fireThreads(metrics, currentTime, 10);
//
//        currentTime = 5600L * 1000L * 1000L;
//        fireThreads(metrics, currentTime, 10);
//
//        long nanoTime = 6000L * 1000L * 1000L;
//        IntervalIterator<PartitionedCount<TimeoutableResult>> intervals = metrics.intervals(nanoTime);
//        Accumulator.Counts<TimeoutableResult> actual = Accumulator.countsForPeriod(intervals, 5, TimeUnit.SECONDS);
//        assertEquals(5000, actual.get(TimeoutableResult.SUCCESS));
//        assertEquals(5000, actual.get(TimeoutableResult.ERROR));
//        assertEquals(5000, actual.get(TimeoutableResult.TIMEOUT));
//
//        intervals = metrics.intervals(nanoTime);
//        actual = Accumulator.countsForPeriod(intervals, 1, TimeUnit.SECONDS);
//        assertEquals(1000, actual.get(TimeoutableResult.SUCCESS));
//        assertEquals(1000, actual.get(TimeoutableResult.ERROR));
//        assertEquals(1000, actual.get(TimeoutableResult.TIMEOUT));
//
//        nanoTime = 6500L * 1000L * 1000L;
//        intervals = metrics.intervals(nanoTime);
//        actual = Accumulator.countsForPeriod(intervals, 5, TimeUnit.SECONDS);
//        assertEquals(4000, actual.get(TimeoutableResult.SUCCESS));
//        assertEquals(4000, actual.get(TimeoutableResult.ERROR));
//        assertEquals(4000, actual.get(TimeoutableResult.TIMEOUT));
//    }
//
//    private static void fireThreads(final RollingCountMetrics<TimeoutableResult> metrics, final long nanoTime, int num) throws
//            InterruptedException {
//        final CountDownLatch latch = new CountDownLatch(num);
//
//        for (int i = 0; i < num; ++i) {
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    for (int j = 0; j < 100; ++j) {
//                        metrics.add(TimeoutableResult.SUCCESS, 1L, nanoTime);
//                        metrics.add(TimeoutableResult.ERROR, 1L, nanoTime);
//                        metrics.add(TimeoutableResult.TIMEOUT, 1L, nanoTime);
//                    }
//                    latch.countDown();
//                }
//            }).start();
//        }
//
//        latch.await();
//    }

    private static long countForPeriod(IntervalIterator<AtomicLong> intervals, long duration, TimeUnit seconds) {
        long total = 0;
        intervals.limit(duration, seconds);
        AtomicLong count;
        while (intervals.hasNext()) {
            count = intervals.next();
            total += count.get();
        }
        return total;
    }

    private static class LongAllocator implements Allocator<AtomicLong> {

        @Override
        public AtomicLong allocateNew() {
            return new AtomicLong(0);
        }
    }

}
