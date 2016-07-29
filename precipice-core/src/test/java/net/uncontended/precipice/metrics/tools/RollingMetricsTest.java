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

import java.util.concurrent.CountDownLatch;
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
        long startTime = 0; // ThreadLocalRandom.current().nextLong();
        CircularBuffer<AtomicLong> buffer = new CircularBuffer<>(2, TimeUnit.SECONDS.toNanos(1), startTime);
        metrics = new RollingMetrics<AtomicLong>(new LongAllocator(), buffer, systemTime);

        long nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(1);
        metrics.current(nanoTime).getAndIncrement();
        nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(2);
        metrics.current(nanoTime).getAndIncrement();

        nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(999L);
        IntervalIterator<AtomicLong> intervals = metrics.intervalsWithDefault(nanoTime, default0);
        assertEquals(2, countForPeriod(intervals, 1, TimeUnit.SECONDS));
        intervals.reset(nanoTime);
        assertEquals(2, countForPeriod(intervals, 2, TimeUnit.SECONDS));

        nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(1000L);
        intervals = metrics.intervalsWithDefault(nanoTime, default0);
        assertEquals(0, countForPeriod(intervals, 1, TimeUnit.SECONDS));
        assertEquals(2, countForPeriod(metrics.intervalsWithDefault(nanoTime, default0), 2, TimeUnit.SECONDS));
//
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
        long startTime = 10000000000000L; // ThreadLocalRandom.current().nextLong();
        long offsetTime = startTime + ThreadLocalRandom.current().nextLong(100);

        CircularBuffer<AtomicLong> buffer = new CircularBuffer<>(1000, TimeUnit.MILLISECONDS.toNanos(100), startTime);
        metrics = new RollingMetrics<AtomicLong>(new LongAllocator(), buffer, systemTime);

        long nanoTime = offsetTime;
        metrics.current(nanoTime).getAndIncrement();

        nanoTime = offsetTime + TimeUnit.MILLISECONDS.toNanos(100);
        IntervalIterator<AtomicLong> intervals = metrics.intervalsWithDefault(nanoTime, default0);
        assertEquals(0, countForPeriod(intervals, 90, TimeUnit.MILLISECONDS));
        intervals.reset(nanoTime);
        assertEquals(1, countForPeriod(intervals, 200, TimeUnit.MILLISECONDS));

        nanoTime = offsetTime + TimeUnit.MILLISECONDS.toNanos(100 * 999);
        intervals.reset(nanoTime);
        assertEquals(1, countForPeriod(intervals, 1000 * 100, TimeUnit.MILLISECONDS));

        nanoTime = offsetTime + TimeUnit.MILLISECONDS.toNanos(100 * 1000);
        intervals.reset(nanoTime);
        assertEquals(0, countForPeriod(intervals, 1000 * 100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testMilliseconddResolution() {
        long startTime = 100L; // ThreadLocalRandom.current().nextLong();
        long offsetTime = startTime + ThreadLocalRandom.current().nextLong(5) + 1;

        CircularBuffer<AtomicLong> buffer = new CircularBuffer<>(10, 10, startTime);
        metrics = new RollingMetrics<AtomicLong>(new LongAllocator(), buffer, systemTime);

        long nanoTime = offsetTime;
        metrics.current(nanoTime).getAndIncrement();

        nanoTime = offsetTime + TimeUnit.NANOSECONDS.toNanos(10);
        IntervalIterator<AtomicLong> intervals = metrics.intervalsWithDefault(nanoTime, default0);
        assertEquals(0, countForPeriod(intervals, 9, TimeUnit.NANOSECONDS));
    }

    @Test
    public void concurrentTest() throws Exception {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long startTime = random.nextLong();

        CircularBuffer<AtomicLong> buffer = new CircularBuffer<>(5, TimeUnit.SECONDS.toNanos(1), startTime);
        metrics = new RollingMetrics<AtomicLong>(new LongAllocator(), buffer, systemTime);

        long nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(random.nextLong(1000L));
        fireThreads(nanoTime, 10);

        nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(random.nextLong(1000L) + 1000L);
        fireThreads(nanoTime, 10);

        nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(random.nextLong(1000L) + 2000L);
        fireThreads(nanoTime, 10);

        nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(random.nextLong(1000L) + 3000L);
        fireThreads(nanoTime, 10);

        nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(random.nextLong(1000L) + 4000L);
        fireThreads(nanoTime, 10);

        nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(random.nextLong(1000L) + 4000L);
        IntervalIterator<AtomicLong> invervals = metrics.intervalsWithDefault(nanoTime, default0);
        assertEquals(5000, countForPeriod(invervals, 5, TimeUnit.SECONDS));

        invervals.reset(nanoTime);
        assertEquals(1000, countForPeriod(invervals, 1, TimeUnit.SECONDS));

        nanoTime = startTime + TimeUnit.MILLISECONDS.toNanos(random.nextLong(1000L) + 5000L);
        invervals.reset(nanoTime);
        assertEquals(4000, countForPeriod(invervals, 5, TimeUnit.SECONDS));
    }

    private void fireThreads(final long nanoTime, int num) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(num);

        for (int i = 0; i < num; ++i) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 100; ++j) {
                        metrics.current(nanoTime).getAndIncrement();
                    }
                    latch.countDown();
                }
            }).start();
        }

        latch.await();
    }

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
