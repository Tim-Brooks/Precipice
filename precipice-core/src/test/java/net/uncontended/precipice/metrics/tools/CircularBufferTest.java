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
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CircularBufferTest {

    private final AtomicLong[] atomicLongs = new AtomicLong[8];
    private final AtomicLong[] expectedValues = new AtomicLong[8];
    private CircularBuffer<AtomicLong> buffer;
    private long startTime;

    // TODO: Merge with RollingMetricsTest

    @Before
    public void setUp() {
        for (int i = 0; i < 8; ++i) {
            atomicLongs[i] = new AtomicLong();
            expectedValues[i] = new AtomicLong();
        }
    }

    @Test
    public void testThatValuesAlign() throws InterruptedException {
        startTime = System.nanoTime();
        buffer = new CircularBuffer<>(4, TimeUnit.SECONDS.toNanos(1), startTime);
        final CountDownLatch latch = new CountDownLatch(5);

        ExecutorService es = Executors.newFixedThreadPool(5);

        for (int i = 0; i < 5; ++i) {
            es.submit(getRunnable(latch));
        }
        latch.await();

        for (int j = 0; j < 4; ++j) {
            long nanoTime = addSeconds(startTime, j);
            AtomicLong atomicLong = buffer.getSlot(nanoTime);
            if (atomicLong == null) {
                atomicLong = buffer.putOrGet(nanoTime, atomicLongs[j]);
            }
            assertEquals(atomicLongs[j], atomicLong);
        }

        for (int j = 4; j < 8; ++j) {
            long nanoTime = addSeconds(startTime, j);
            AtomicLong atomicLong = buffer.getSlot(nanoTime);
            if (atomicLong == null) {
                atomicLong = buffer.putOrGet(nanoTime, atomicLongs[j]);
            }
            assertNotNull(atomicLong);
        }


        for (int i = 0; i < 8; ++i) {
            assertEquals(expectedValues[i].longValue(), atomicLongs[i].longValue());
        }
    }

    @Test
    public void testIntervalTimes() throws InterruptedException {
        startTime = ThreadLocalRandom.current().nextLong();
        buffer = new CircularBuffer<>(4, TimeUnit.SECONDS.toNanos(1), startTime);

        final CountDownLatch latch = new CountDownLatch(5);

        ExecutorService es = Executors.newFixedThreadPool(5);

        for (int i = 0; i < 5; ++i) {
            es.submit(getRunnable(latch));
        }
        latch.await();

        long remainder = TimeUnit.MILLISECONDS.toNanos(ThreadLocalRandom.current().nextLong(1000));
        long nanoTime = startTime + TimeUnit.SECONDS.toNanos(5) + remainder;
        IntervalIterator<AtomicLong> intervals = buffer.intervals(nanoTime, new AtomicLong(0));

        int i = 3;
        while (intervals.hasNext()) {
            assertEquals(- (i * TimeUnit.SECONDS.toNanos(1) + remainder), intervals.intervalStart());
            long intervalEnd = -((i - 1) * TimeUnit.SECONDS.toNanos(1) + remainder);
            assertEquals(intervalEnd < 0 ? intervalEnd : 0, intervals.intervalEnd());
            intervals.next();
            --i;
        }
    }

    @Test
    public void testLimitInterval() throws InterruptedException {
        startTime = 0;
        buffer = new CircularBuffer<>(4, TimeUnit.SECONDS.toNanos(1), startTime);

//        final CountDownLatch latch = new CountDownLatch(5);
//
//        ExecutorService es = Executors.newFixedThreadPool(5);
//
//        for (int i = 0; i < 5; ++i) {
//            es.submit(getRunnable(latch));
//        }
//        latch.await();

        long remainder = TimeUnit.MILLISECONDS.toNanos(100);
        long nanoTime = TimeUnit.SECONDS.toNanos(4) + remainder;
        IntervalIterator<AtomicLong> intervals = buffer.intervals(nanoTime, new AtomicLong(0));

        intervals.limit(2200, TimeUnit.MILLISECONDS);


        while (intervals.hasNext()) {
            System.out.println(nanoTime + intervals.intervalStart());
            System.out.println(nanoTime + intervals.intervalEnd());
            intervals.next();
        }
    }

    private Runnable getRunnable(final CountDownLatch latch) {
        return new Runnable() {
            @Override
            public void run() {
                for (int j = 0; j < 8; ++j) {
                    int bound = ThreadLocalRandom.current().nextInt(500);
                    expectedValues[j].addAndGet(bound);
                    for (int i = 0; i < bound; ++i) {
                        long nanoTime = addSeconds(startTime, j);
                        AtomicLong atomicLong = buffer.getSlot(nanoTime);
                        if (atomicLong == null) {
                            atomicLong = buffer.putOrGet(nanoTime, atomicLongs[j]);
                        }
                        if (atomicLong != null) {
                            atomicLong.addAndGet(1);
                        }
                    }
                }
                latch.countDown();
            }
        };
    }

    private static long addSeconds(long startTime, int seconds) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long bufferSeconds = random.nextLong(TimeUnit.MILLISECONDS.toNanos(999));
        return startTime + TimeUnit.SECONDS.toNanos(seconds) + bufferSeconds;
    }
}
