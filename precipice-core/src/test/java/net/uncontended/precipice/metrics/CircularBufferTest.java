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
package net.uncontended.precipice.metrics;

import net.uncontended.precipice.metrics.tools.CircularBuffer;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CircularBufferTest {

    private AtomicLong[] atomicLongs = new AtomicLong[8];
    private AtomicLong[] expectedValues = new AtomicLong[8];
    private AtomicLong[] missed = new AtomicLong[8];
    private CircularBuffer<AtomicLong> buffer;
    private long startTime;

    @Before
    public void setUp() {
        for (int i = 0; i < 8; ++i) {
            atomicLongs[i] = new AtomicLong();
            expectedValues[i] = new AtomicLong();
            missed[i] = new AtomicLong();
        }
    }

    @Test
    public void thing() {
        startTime = 0;
        buffer = new CircularBuffer<>(4, TimeUnit.SECONDS.toNanos(1), startTime);
        long resolution = TimeUnit.SECONDS.toNanos(1);

        for (int i = 3; i >= 0; --i) {
            long nanoTime = Long.MAX_VALUE - (resolution * i) + resolution;
            buffer.putOrGet(nanoTime, new AtomicLong(i));
        }

        IntervalIterator<AtomicLong> intervals = buffer.intervals(Long.MAX_VALUE + resolution, null);
        while (intervals.hasNext()) {
            System.out.println("\n");
            System.out.println(intervals.next());
            System.out.println(intervals.intervalStart());
            System.out.println(intervals.intervalEnd());
            System.out.println(intervals.intervalEnd() - intervals.intervalStart());
            System.out.println("\n");
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
            assertEquals(expectedValues[i].longValue(), atomicLongs[i].longValue() + missed[i].longValue());
        }
    }

    @Test
    public void testIntervalTimes() throws InterruptedException {
        startTime = System.nanoTime() - TimeUnit.SECONDS.toNanos(10);
        buffer = new CircularBuffer<>(4, TimeUnit.SECONDS.toNanos(1), startTime);

        // TODO: Implement
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
                        } else {
                            missed[j].addAndGet(1);
                        }
                    }
                }
                latch.countDown();
            }
        };
    }


    private long addSeconds(long startTime, int seconds) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        long bufferSeconds = random.nextLong(TimeUnit.MILLISECONDS.toNanos(500));
        return startTime + TimeUnit.SECONDS.toNanos(seconds) + bufferSeconds;

    }
}
